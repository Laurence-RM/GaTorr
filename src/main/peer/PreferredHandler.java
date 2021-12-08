package main.peer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import main.peer.message.Choke;
import main.peer.message.Unchoke;
import main.peer.peerProcess.PeerInfo;

public class PreferredHandler extends Thread {
    enum Type {
        CHOKED,
        PREFERRED,
        OPTIMISTIC
    }
    
    private HashMap<PeerInfo, Type> neighbors;
    private peerProcess process;
    private int numPreferredNeighbors;
    private int unchokeInterval;
    private int optimisticInterval;
    private boolean complete;

    Random rnd = new Random();

    public PreferredHandler(PeerInfo[] peers, peerProcess process_, int numPreferredNeighbors_, int unchokeInterval_, int optimisticInterval_, boolean complete_) {
        this.neighbors = new HashMap<PeerInfo, Type>();
        this.process = process_;
        this.numPreferredNeighbors = numPreferredNeighbors_;
        this.unchokeInterval = unchokeInterval_;
        this.optimisticInterval = optimisticInterval_;
        this.complete = complete_;

        if (peers != null) {
            for (PeerInfo peer : peers) {
                // Check if actually choked?
                neighbors.put(peer, Type.CHOKED);
            }
        }
    }

    public void addNeighbor(PeerInfo peer) {
        synchronized (neighbors) {
            if (!neighbors.containsKey(peer)) {
                neighbors.put(peer, Type.CHOKED);
            }
        }
    }

    public void removeNeighbor(PeerInfo p) {
        synchronized (neighbors) {
            neighbors.remove(p);
        }
    }

    public void setComplete() {
        this.complete = true;
    }

    public synchronized boolean isChoked(PeerInfo peer) {
        if (neighbors.get(peer) == Type.CHOKED) {
            return true;
        }
        return false;
    }

    public synchronized void setChoked(PeerInfo peer) {
        neighbors.replace(peer, Type.CHOKED);
        peer.isChoking = true;
        peer.sendMessage(new Choke());
    }

    public synchronized void setPreferred(PeerInfo peer) {
        // Don't send unchoke msg if already unchoked
        if (isChoked(peer)) {
            neighbors.replace(peer, Type.PREFERRED);
            peer.isChoking = false;
            peer.sendMessage(new Unchoke());
        } else {
            neighbors.replace(peer, Type.PREFERRED);
            peer.isChoking = false;
        }

    }

    public synchronized void setOptimistic(PeerInfo peer) {
        if (isChoked(peer)) {
            neighbors.replace(peer, Type.OPTIMISTIC);
            peer.isChoking = false;
            peer.sendMessage(new Unchoke());
        } else {
            neighbors.replace(peer, Type.OPTIMISTIC);
            peer.isChoking = false;
        }
    }

    public void run() {
        while (true) {
            try {
                if (neighbors.size() == 0) {
                    Thread.sleep(unchokeInterval * 1000);
                    continue;
                }

                ArrayList<PeerInfo> potentialPreferredNeighbors = new ArrayList<PeerInfo>();
                for (PeerInfo peer : neighbors.keySet()) {
                    if (peer.isInterested) {
                        potentialPreferredNeighbors.add(peer);
                    }
                }

                // sort by download rate
                potentialPreferredNeighbors.sort((p1, p2) -> p1.downloadRate > p2.downloadRate ? 1 : -1);

                boolean peersChanged = false;
                for (int i = 0; i < numPreferredNeighbors; i++) {
                    if (potentialPreferredNeighbors.size() > 0) {
                        PeerInfo peer;
                        if (complete) {
                            peer = potentialPreferredNeighbors.get(rnd.nextInt(potentialPreferredNeighbors.size()));
                        } else {
                            // TODO: Get random if other peers have same download rate
                            peer = potentialPreferredNeighbors.get(0);
                        }
                        // Check if peers changed
                        if (neighbors.get(peer) != Type.PREFERRED) {
                            peersChanged = true;
                        }
                        setPreferred(peer);
                        potentialPreferredNeighbors.remove(0);
                    } else {
                        break;
                    }
                }

                for (PeerInfo peer : potentialPreferredNeighbors) {
                    if (neighbors.get(peer) == Type.PREFERRED) {
                        setChoked(peer);
                    }
                }

                ArrayList<String> logNeighbors = new ArrayList<String>();

                for (PeerInfo peer : neighbors.keySet()) {
                    if (neighbors.get(peer) == Type.PREFERRED) {
                        logNeighbors.add(Integer.toString(peer.ID));
                    }
                    peer.downloadRate = 0;
                }

                if (peersChanged) {
                    process.writeToLog("has the preferred neighbors " + String.join(", ", logNeighbors));
                }
                Thread.sleep(unchokeInterval * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public class OptimisticHandler extends Thread {

        public void run() {
            while (true) {
                try {
                    if (neighbors.size() == 0) {
                        Thread.sleep(optimisticInterval * 1000);
                        continue;
                    }

                    ArrayList<PeerInfo> potentialOpts = new ArrayList<PeerInfo>();
                    PeerInfo oldOptPeer = null;

                    // Populate list with choked and interested peers
                    for (PeerInfo peer : neighbors.keySet()) {
                        if (neighbors.get(peer) == Type.OPTIMISTIC) {
                            oldOptPeer = peer;
                            if (peer.isInterested) {
                                potentialOpts.add(peer);
                            }
                            continue;
                        }

                        if (isChoked(peer) && peer.isInterested) {
                            potentialOpts.add(peer);
                        }
                    }

                    // Set random optimistic neighbor
                    if (potentialOpts.size() > 0) {
                        PeerInfo optimistic = potentialOpts.get(rnd.nextInt(potentialOpts.size()));
                        if (neighbors.get(optimistic) != Type.OPTIMISTIC) {
                            if (oldOptPeer != null) {
                                setChoked(oldOptPeer);
                            }
                            setOptimistic(optimistic);
                            process.writeToLog("has the optimistically unchoked neighbor " + optimistic.ID + ".");
                        }
                    }
                    Thread.sleep(optimisticInterval * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    
}

/*
    Peer Process

    * Takes peerID as parameter.
    * Reads Common.cfg to set variables
    * Reads PeerInfo.cfg to find other peers that have file
    * Set bits of bitfield to 0 or 1 depending on above
    * Listen on port for other peers if first
    * Make connections to already started peers
    * Terminate when all peers have complete file

*/

package main.peer;

import java.util.ArrayList;
import java.util.Scanner;

import main.peer.message.Handshake;
import main.peer.message.Message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.*;

public class Peer {

    private int peerID;
    private String hostName;
    private int port;

    private int numPreferredNeighbors;
    private int unchokingInterval;
    private int optimisticUnchokingInterval;
    private String fileName;
    private int fileSize;
    private int pieceSize;

    private int pieceCount;
    private int lastPieceSize;

    private BitfieldObj bitfield;
    private ArrayList<PeerInfo> priorPeers;

    public class PeerInfo {
        public int ID;
        public String hostname;
        public int port;
        public boolean complete = false;
        public DataInputStream in_steam;
        public DataOutputStream out_stream;
        public Socket connection;

        public PeerInfo(String[] info) {
            this.ID = Integer.parseInt(info[0]);
            this.hostname = info[1];
            this.port = Integer.parseInt(info[2]);
            if (info[3].equals("1")) {
                this.complete = true;
            }
        }

        protected void finalize() {
            try {
                this.connection.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                System.out.printf("Connection with peer %s closed from %s:%d", this.ID, this.hostname, this.port);
            }
        }

        public void establishConnection(Socket s) {
            try {
                this.connection = new Socket(this.hostname, this.port);
                this.out_stream = new DataOutputStream(this.connection.getOutputStream());
                this.out_stream.flush();
                this.in_steam = new DataInputStream(this.connection.getInputStream());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void sendMessage(Message m) {
            try {
                out_stream.write(m.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void sendMessage(Handshake h) {
            try {
                out_stream.write(h.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void sendMessage(byte[] b) {
            try {
                out_stream.write(b);
            } catch (Exception e) {
                e.printStackTrace();
            }            
        }
    }

    public Peer(int id) {
        this.setPeerID(id);

        // Read common cfg
        Scanner reader = null;
        try {
            File commonCfg = new File("Common.cfg");
            reader = new Scanner(commonCfg);
            while (reader.hasNextLine()) {
                String property = reader.next();

                switch (property) {
                    case "NumberOfPreferredNeighbors":
                        this.numPreferredNeighbors = reader.nextInt();
                        break;
                    case "UnchokingInterval":
                        this.unchokingInterval = reader.nextInt();
                        break;
                    case "OptimisticUnchokingInterval":
                        this.optimisticUnchokingInterval = reader.nextInt();
                        break;
                    case "FileName":
                        this.fileName = reader.next();
                        break;
                    case "FileSize":
                        this.fileSize = reader.nextInt();
                        break;
                    case "PieceSize":
                        this.pieceSize = reader.nextInt();
                        break;
                    default:
                        break;
                }
            }
        } catch (FileNotFoundException e) {
            //e.printStackTrace();
            System.out.println("Common cfg not found.");
            return;
        } finally {
            try { 
                reader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            pieceCount = (int) Math.ceil(this.fileSize / this.pieceSize); // Number of pieces
            lastPieceSize = this.fileSize - ((this.pieceCount - 1) * this.pieceSize); // Find remainder size of last piece
        }
        
        // Read peer cfg
        readPeerInfo();
    }
    
    public int getPeerID() {
        return peerID;
    }

    private void setPeerID(int peerID) {
        this.peerID = peerID;
    }

    private void readPeerInfo() {
        // Read information from peer config file
        Scanner reader = null;
        try {
            File cfg = new File("PeerInfo.cfg");
            reader = new Scanner(cfg);

            boolean selfFound = false;
            priorPeers = new ArrayList<PeerInfo>();
            
            while (reader.hasNextLine()) {
                String[] peerInfo_str = reader.nextLine().split(" ", 4);
                PeerInfo p = new PeerInfo(peerInfo_str);
                
                if (p.ID == peerID) {
                    // Create directory
                    File pf = new File("peer_" + p.ID);
                    if (pf.mkdir()) {
                        System.out.println("Created directory for peer " + p.ID);
                    }
                    // Save properties
                    this.hostName = p.hostname;
                    this.port = p.port;
                    // Prepare Bitfield
                    if (p.complete) {
                        this.bitfield = new BitfieldObj(pieceCount, true);
                        
                        //TODO: Check that file is actually complete
                    }
                    selfFound = true;
                    break;
                }
                else {
                    // Save previous peers on list to connect to later
                    priorPeers.add(p);
                }
            }
            if (!selfFound) {
                System.out.printf("Peer %d not found in PeerInfo.cfg, could not start process.\n", peerID);
                reader.close();
                System.exit(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Handler for incoming peer connections
    private class Handler extends Thread {
        private Socket connection;
        private int ID;
        private DataInputStream in;
        private DataOutputStream out;

        public Handler(Socket connection) {
            this.connection = connection;
        }

        public void run() {
            try {
                // Initialize streams
                out = new DataOutputStream(connection.getOutputStream());
                out.flush();
                in = new DataInputStream(connection.getInputStream());
                try {
                    // Wait for handshake
                    while(true) {
                        byte[] handshakeMsg = new byte[32];
                        // Receive hs message
                        in.readFully(handshakeMsg);
                        this.ID = Handshake.validateMessage(handshakeMsg);

                        if (ID != -1) {
                            //Send back handshake
                            System.out.println("Handshake received from peer "+ID);
                            break;
                        } else {
                            System.out.println("Invalid Handshake received...");
                            continue;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    in.close();
                    out.close();
                    connection.close();
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        // Create peer for peerProcess
        // Check configs
        // Send HS to prior peers (or skip if none)
        // Wait for responses
        try{
            Peer peer = new Peer(Integer.parseInt(args[0]));
            ServerSocket listener = new ServerSocket(peer.port);

            for (PeerInfo p : peer.priorPeers) {
                System.out.println("Attempting handshake with peer " + p.ID);
                try {
                    // Send handshake
                    Handshake hs = new Handshake(peer.peerID);
                    p.sendMessage(hs);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            try {
                while(true) {
                    peer.new Handler(listener.accept()).start();
                }
            } finally {
                listener.close();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            //System.out.println("Improper args. Missing numerical Peer ID");
        }
    }
}
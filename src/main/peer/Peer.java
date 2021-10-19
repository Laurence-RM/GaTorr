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
import test.Server;

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
    private ArrayList<String[]> priorPeers;

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
        Scanner reader = null;
        try {
            File cfg = new File("PeerInfo.cfg");
            reader = new Scanner(cfg);

            boolean selfFound = false;
            priorPeers = new ArrayList<String[]>();

            while (reader.hasNextLine()) {
                String[] peerInfo = reader.nextLine().split(" ", 4);

                if (peerInfo[0].equals(Integer.toString(peerID))) {
                    // Create directory
                    File pf = new File("peer_" + peerInfo[0]);
                    if (pf.mkdir()) {
                        System.out.println("Created directory for peer " + peerInfo[0]);
                    }
                    // Save properties
                    this.hostName = peerInfo[1];
                    this.port = Integer.parseInt(peerInfo[2]);
                    // Prepare Bitfield
                    if (peerInfo[3].equals("1")) {
                        this.bitfield = new BitfieldObj(pieceCount, true);

                        //TODO: Check that file is actually complete
                    }
                    selfFound = true;
                    break;
                }
                else {
                    // Save previous peers on list to connect to later
                    priorPeers.add(peerInfo);
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
        private int clientNumber;
        private int ID;
        private DataInputStream in;
        private DataOutputStream out;

        public Handler(Socket connection, int cliNum) {
            this.connection = connection;
            this.clientNumber = cliNum;
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
            int cliNumber = 1;

            for (String[] p : peer.priorPeers) {
                System.out.println("Attempting handshake with peer "+p[0]);
                try {
                    Socket request = new Socket(p[1], Integer.parseInt(p[2]));
                    DataOutputStream out = new DataOutputStream(request.getOutputStream());
                    out.flush();
                    DataInputStream in = new DataInputStream(request.getInputStream());

                    // Send handshake
                    Handshake hs = new Handshake(peer.peerID);
                    out.write(hs.getMessage());
                    request.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            try {
                while(true) {
                    peer.new Handler(listener.accept(), cliNumber).start();;
                    System.out.println("Client connected: #" + cliNumber++);
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
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
import java.util.Date;
import java.util.Scanner;

import main.peer.TorrentFile.PieceObj;
import main.peer.message.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.text.SimpleDateFormat;

public class peerProcess {

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

    private File logFile;
    private FileOutputStream logStream;
    private SimpleDateFormat dateFormat;

    private TorrentFile torrentFile;

    public void writeToLog(String msg) {
        // Will append Date + PeerID + Message to log file
        try {
            if (logStream == null) {
                logFile = new File("log_peer_" + peerID + ".log");
                logFile.delete(); //Delete old log file
                logStream = new FileOutputStream(logFile, true);
                dateFormat = new SimpleDateFormat("[yy-MM-dd HH:mm:ss]: ");
            }
            Date date = new Date();
            // Append msg to log
            String logMsg = dateFormat.format(date) + "Peer " + peerID + " " + msg + "\n";
            logStream.write(logMsg.getBytes());
        } catch (IOException e) {
            System.out.println("Error creating/writing log.");
        }
    }

    public class PeerInfo {
        public int ID;  // TODO: Should ID be string to store leading zeros?
        public String hostname;
        public int port;
        public boolean complete = false;
        public BitfieldObj bf;

        public DataInputStream in;
        public DataOutputStream out;
        public Socket connection;

        public PeerInfo(String[] info) {
            this.ID = Integer.parseInt(info[0]);
            this.hostname = info[1];
            this.port = Integer.parseInt(info[2]);
            if (info[3].equals("1")) {
                this.complete = true;
            }
        }

        public PeerInfo() {}

        protected void finalize() {
            try {
                this.connection.close();
            } catch (IOException e) {
                System.out.println("No connection with peer "+ID+" to close.");
                return;
            }
            finally {
                System.out.printf("Connection with peer %s closed from %s:%d\n", this.ID, this.hostname, this.port);
            }
        }

        public void establishConnection() {
            try {
                if (connection == null) {
                    this.connection = new Socket(this.hostname, this.port);
                }
                this.out = new DataOutputStream(this.connection.getOutputStream());
                this.out.flush();
                this.in = new DataInputStream(this.connection.getInputStream());
            } catch (IOException e) {
                System.out.println("Error establishing connection with peer " + ID);
                this.connection = null;
            }
        }

        public void establishConnection(Socket s) {
            this.connection = s;
            establishConnection();
        }

        public void sendMessage(Message m) {
            try {
                out.write(m.getMessage());
            } catch (IOException e) {
                System.out.println("Connection Error: Could not send message to peer "+ID);
            }
        }

        public void sendMessage(Handshake h) {
            try {
                out.write(h.getMessage());
            } catch (IOException e) {
                System.out.println("Connection Error: Could not send message to peer " + ID);
            }
        }

        public void sendMessage(byte[] b) {
            try {
                out.write(b);
            } catch (IOException e) {
                System.out.println("Connection Error: Could not send message to peer " + ID);
            }         
        }
    }

    public peerProcess(int id) {
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
            lastPieceSize = this.fileSize % this.pieceSize; // Find remainder size of last piece
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
                    // Prepare torrent file
                    pf = new File(pf, fileName);
                    this.torrentFile = new TorrentFile(fileName, fileSize, lastPieceSize, pf);
                    // Prepare Bitfield
                    if (p.complete) {
                        this.bitfield = new BitfieldObj(pieceCount, true);    
                        torrentFile.setBitfield(bitfield);
                        if (!torrentFile.isComplete()) {
                            System.out.println("File for peer" + p.ID + " does not exist or is not complete.\nCould not start process.");
                            System.exit(1);
                        }
                        System.out.println("File confirmed for peer " + p.ID);            
                    }
                    else {
                        this.bitfield = new BitfieldObj(pieceCount);
                        torrentFile.setBitfield(bitfield);
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
                System.exit(1);
            }
        } catch (FileNotFoundException e) {
            System.out.println("PeerInfo.cfg not found, could not start process.");
            System.exit(1);
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
        PeerInfo p;
        boolean shook = false;

        public Handler(Socket connection) {
            this.p = new PeerInfo();
            p.ID = 0;

            // Initialize streams
            p.establishConnection(connection);
            p.hostname = connection.getRemoteSocketAddress().toString();
            p.port = connection.getPort();
        }

        public Handler(PeerInfo p_) {
            this.p = p_;
            this.shook = true;
        }

        public void run() {
            try {
                // Receive handshake message
                byte[] handshakeMsg = new byte[32];
                p.in.readFully(handshakeMsg); // Blocks thread until 32 bytes are available

                // Validate handshake
                int id_in = Handshake.validateMessage(handshakeMsg);
                if (id_in != -1) {
                    if (!shook) {
                        writeToLog(String.format("is connected from Peer %d", id_in));
                        //Send back handshake
                        p.sendMessage(new Handshake(peerID));
                        p.ID = id_in;
                    } else {
                        if (id_in != p.ID) {
                            // Received peer ID does not match expected ID
                            System.out.printf("ERROR: Expected peer %d but received %d during Handshake", p.ID, id_in);
                        } else {
                            System.out.println("Handshake from " + p.ID + " confirmed.");
                        }
                    }
                } else {
                    System.out.println("Invalid Handshake received...");
                    p.finalize();
                    return;
                }

                // Exchange bitfields
                p.sendMessage(new Bitfield(bitfield));
                
                // Read in length and type
                int len = p.in.readInt();
                byte type = p.in.readByte();

                // Check type matches bitfield
                if (type == Message.BITFIELD) {
                    byte[] bf_data = new byte[len-1];
                    p.in.readFully(bf_data);
                    p.bf = new BitfieldObj(bf_data, pieceCount);
                    System.out.printf("Bitfield received from peer %d\n", p.ID);
                    //p.bf.printData();
                }
                
                // Send Interested or Not Interested based on bitfield
                if (p.bf.hasPiece(bitfield)) {
                    p.sendMessage(new Interested());
                    System.out.println("Sending interested msg to " + p.ID);
                } else {
                    p.sendMessage(new NotInterested());
                    System.out.println("Sending not interested msg to " + p.ID);
                }

                // Wait for other messages
                while(true) {
                    len = p.in.readInt();
                    type = p.in.readByte();
                    Message msg;
                    byte[] data;
                    if (type >= 0 && type <= 7 && len > 0) {
                        data = new byte[len-1];
                        p.in.readFully(data);
                    }
                    else {
                        data = null;
                    }
                    msg = new Message(type, data);
                    switch (type) {
                        case Message.CHOKE:
                            // Handle choke msg here
                            writeToLog("is choked by " + p.ID);
                            break;
                        case Message.UNCHOKE:
                            // Handle unchoke
                            writeToLog("is unchoked by " + p.ID);
                            break;
                        case Message.INTERESTED:
                            // Handle interested
                            writeToLog("received the 'interested' message from " + p.ID);
                            Piece p_ = torrentFile.getPieceFromFile(1).getPieceMsg();
                            p.sendMessage(p_);
                            break; 
                        case Message.NOTINTERESTED:
                            // Handle notinterested
                            writeToLog("received the 'not interested' message from " + p.ID);
                            break;
                        case Message.HAVE:
                            // Handle have
                            Have have_msg = new Have(msg);
                            writeToLog(String.format("received the 'have' message from %d for the piece %d", p.ID, have_msg.getIndex()));
                            break;
                        case Message.BITFIELD:
                            // Handle bitfield
                            System.out.println("Received bitfield msg from " + p.ID);
                            // Note: should not be receiving more bitfields after first
                            break;  
                        case Message.REQUEST:
                            // Handle request
                            Request req_msg = new Request(msg);
                            writeToLog(String.format("has requested the piece %d from %d", req_msg.getIndex(), p.ID));                          
                            break;
                        case Message.PIECE:
                            // Handle piece
                            Piece piece_msg = new Piece(msg);
                            // TODO: write content to file
                            PieceObj piece = torrentFile.new PieceObj(piece_msg);
                            torrentFile.writePieceToFile(piece);
                            writeToLog(String.format("has downloaded the piece %d from %d", piece_msg.getIndex(), p.ID));                            
                            break;
                        default:
                            // Could not identify message type
                            System.out.println("Received unknown msg from " + p.ID);
                            break;
                    }
                }
            } catch (IOException e) {
                System.out.println("Peer "+p.ID+" disconnected.");
            }
        }
    }

    public static void main(String[] args) {
        // Create peer for peerProcess
        // Check configs
        // Send HS to prior peers (or skip if none)
        // Wait for responses
        try{
            peerProcess peer = new peerProcess(Integer.parseInt(args[0]));
            ServerSocket listener = new ServerSocket(peer.port);

            for (PeerInfo p : peer.priorPeers) {
                int connectionAttempts = 0;
                System.out.println("Attempting handshake with peer " + p.ID);
                while (true) {
                    // Connect to peer
                    p.establishConnection();
                    if (p.connection != null) {
                        // Send handshake
                        peer.writeToLog(String.format("makes a connection to Peer %d", p.ID));
                        Handshake hs = new Handshake(peer.peerID);
                        p.sendMessage(hs);
                        peer.new Handler(p).start();
                        break;
                    } else if (connectionAttempts >= 10) {
                        System.out.println("Could not connect to peer " + p.ID);
                        peer.writeToLog(String.format("could not connect to Peer %d after %d attempts", p.ID, connectionAttempts));
                        break;
                    } else {
                        // Could not connect to peer
                        connectionAttempts++;
                        System.out.println("("+connectionAttempts+"/10) Retrying handshake with peer " + p.ID + " in 5 seconds...");
                        Thread.sleep(5000);
                        continue;
                    }
                }
            }
            try {
                System.out.println("Listening for peers on port " + peer.port);
                while(true) {
                    peer.new Handler(listener.accept()).start(); // Blocks until connection attempted
                    // System.out.println("New connection...");
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
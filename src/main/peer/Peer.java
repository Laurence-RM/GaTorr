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

import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;

public class Peer {

    private int peerID;

    private int numPreferredNeighbors;
    private int unchokingInterval;
    private int optimisticUnchokingInterval;
    private String fileName;
    private int fileSize;
    private int pieceSize;

    public Peer(int id) {
        this.peerID = id;

        // Read common cfg
        try {
            File commonCfg = new File("Common.cfg");
            Scanner reader = new Scanner(commonCfg);
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
            reader.close();
        } catch (FileNotFoundException e) {
            //e.printStackTrace();
            System.out.println("Common cfg not found.");
        }
    }

    public static void main(String[] args) {
        try{
            Peer pTest = new Peer(Integer.parseInt(args[0]));
        } catch (Exception e) {
            e.printStackTrace();
            //System.out.println("Improper args. Missing numerical Peer ID");
        }
    }
}
/*
    Handshake Message
        Establishes connection between peers (Hi I'm peer 1001)
    * 18-byte Handshake Header ('P2PFILESHARINGPROJ')
    * 10-byte zero bits
    * 4-byte peer ID

*/

package main.peer.message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import main.Utils;

public class Handshake {
    private static String HEADER = "P2PFILESHARINGPROJ";
    private int peerID;

    public Handshake(int ID) {
        this.peerID = ID;
    }

    public byte[] getMessage() {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream(32);
        outBytes.write(HEADER.getBytes(), 0, 18);
        
        byte[] zeros = new byte[10];
        Arrays.fill(zeros, (byte) 0);
        outBytes.write(zeros, 0, 10);
        outBytes.write(Utils.intToByteArray(peerID), 0, 4);

        return outBytes.toByteArray();
    }

    public static int validateMessage(byte[] msg) {
        if (msg.length != 32) { return -1; }
        ByteArrayInputStream inBytes = new ByteArrayInputStream(msg);
        byte[] bHeader = new byte[18];
        inBytes.read(bHeader, 0, 18);
        if (!new String(bHeader).equals(HEADER)) {
            return -1;
        }

        for (int i = 0; i < 10; i++) {
            if (inBytes.read() != 0) {
                return -1;
            }
        }

        byte[] id = new byte[4];
        inBytes.read(id, 0, 4);
        return Utils.byteArrayToInt(id);
    }

    public static void main(String args[]) {
        Handshake hs = new Handshake(1001);
        int res = validateMessage(hs.getMessage());
        System.out.println(res);
    }
}

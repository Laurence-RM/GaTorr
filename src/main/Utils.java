/*
    Class containing tools/methods to solve repetitive tasks
*/

package main;


public class Utils {
    
    // Convert a 4-byte integer into a 4 byte array
    public static byte[] intToByteArray(int num) {
        byte[] ba = new byte[4];
        for (int i = 3; i >= 0; i--) {
            ba[3-i] = (byte) (num >> i*8);
        }
        return ba;
    }

    public static final int byteArrayToInt(byte[] ba) {
        if (ba.length != 4) { return -1; }

        int out = 0;
        for (int i = 3; i >= 0; i--) {
            out |= ((ba[3-i] & 0xFF) << i*8);
        }
        return out;
    }

    public static void main(String args[]) {
        byte[] b = intToByteArray(1234);
        for (byte B : b) {
            System.out.printf("%02x ", B);
        }
        System.out.println("\n"+byteArrayToInt(b));
    }
}

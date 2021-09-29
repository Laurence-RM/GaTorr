/*
    Class containing tools/methods to solve repetitive tasks
*/

package main;


public class Utils {
    
    // Convert a 4-byte integer into a 4 byte array
    public static byte[] intToByteArray(int num) {
        byte[] ba = new byte[4];
        for (int i = 3; i >= 0; i--) {
            ba[i] = (byte) (num >>> i*8);
        }
        return ba;
    }
}

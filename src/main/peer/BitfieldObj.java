package main.peer;

public class BitfieldObj {

    private byte[] data = null;
    private int leftoverBits = 0;
    
    public BitfieldObj(int size) {
        // Size corresponds to the number of bits
        if (size <= 0) { throw new IllegalArgumentException("Bitfield needs to be larger than 0."); }

        setData(new byte[(int) Math.ceil(size/8)]); // create an array of bytes that can support the number of bits 
        leftoverBits = size - ((data.length-1)*8);       
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public boolean checkBit(int index) {
        // index of 0 = 1st bit in bitfield

        if (Math.ceil(index / 8) <= data.length && index >= 0) {
            // index is within bitfield bounds
            int i = (int) index/8; // index of corresponding byte in data
            int j = 8*(i+1)-index-1; // reverse position of wanted bit: second bit in byte is 2^6, j=6
            int result = data[i] & 2^(j); // Bitwise AND to check bit is 0 or 1
            result >>= j; // bit shift right to rightmost bit 

            if (result == 1) { 
                return true;
            }
        }
        return false; // May want to throw exception instead if out of bounds
    }

    public boolean setBit(int index) {
        if (Math.ceil(index / 8) <= data.length && index >= 0) {
            // index is within bitfield bounds
            int i = (int) index/8; // index of corresponding byte in data
            int j = 8*(i+1)-index-1; // reverse position of wanted bit: second bit in byte is 2^6, j=6
            data[i] |= 2^(j); // Bitwise OR and assign bit value
            return true;
        }
        return false;
    }

    public boolean isComplete() {
        // Check full bytes
        for (int i = 0; i < data.length-1; i++) {
            if (data[i] != 255) {
                return false;
            }
        }

        // Check last incomplete byte
        for (int j = 0; j < leftoverBits-1; j++) {
            if (!checkBit((data.length-1)*8 + j)) {
                return false;
            }
        }
        return true;
    }
}

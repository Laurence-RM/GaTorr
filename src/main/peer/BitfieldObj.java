package main.peer;

import java.util.Arrays;
import java.util.Iterator;

public class BitfieldObj implements Iterable<Boolean> {

    private byte[] data = null;
    private int leftoverBits = 0;
    
    public BitfieldObj(int size, boolean full) {
        // Size corresponds to the number of bits/pieces
        if (size <= 0) { throw new IllegalArgumentException("Bitfield needs to be larger than 0."); }

        setData(new byte[(int) Math.ceil(((float) size)/8)]); // create an array of bytes that can support the number of bits 
        leftoverBits = size % 8;;   
        
        if (full) {
            Arrays.fill(data, (byte) 0xff);
            byte temp = 0;
            for (int i = 7; i > 7-leftoverBits; i--) {
                temp+= (int) Math.pow(2,i);
            }
            data[data.length-1] = temp;
        }
    }

    public BitfieldObj(int size) {
        this(size, false);
    }

    public BitfieldObj(byte[] data_, int size) {
        this.data = data_;
        this.leftoverBits = size - ((data.length) * 8);
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public boolean checkBit(int index) {
        // index of 0 = 1st bit in bitfield

        if (Math.ceil(((float) index) / 8) <= data.length && index >= 0) {
            // index is within bitfield bounds
            int i = index/8; // index of corresponding byte in data
            int j = 8*(i+1)-index-1; // reverse position of wanted bit: second bit in byte is 2^6, j=6
            int result = data[i] & (1 << j); // Bitwise AND to check bit is 0 or 1
            result >>= j; // bit shift right to rightmost bit 

            if (result == 1) { 
                return true;
            }
        }
        return false; // May want to throw exception instead if out of bounds
    }

    public boolean setBit(int index) {
        if (Math.ceil(((float) index) / 8) <= data.length && index >= 0) {
            // index is within bitfield bounds
            int i = (int) index/8; // index of corresponding byte in data
            int j = 8*(i+1)-index-1; // reverse position of wanted bit: second bit in byte is 2^6, j=6
            data[i] |= 1 << j; // Bitwise OR and assign bit value
            return true;
        }
        return false;
    }

    public boolean isComplete() {
        // Check full bytes
        for (int i = 0; i < data.length-1; i++) {
            if (data[i] != -1) {
                return false;
            }
        }

        // Check last incomplete byte
        for (int j = 0; j < leftoverBits; j++) {
            if (!checkBit((data.length-1)*8 + j)) {
                return false;
            }
        }
        return true;
    }

    // for testing
    public void printData() {
        for (int j = 0; j < data.length; j++) {
            System.out.format("%02X ", data[j]);
        }
        System.out.println();
    }

    // Check if bitfield contains missing pieces
    public boolean hasPiece(BitfieldObj bitfield) {
        // Compare bitfield lengths
        if (bitfield.getData().length - bitfield.leftoverBits != data.length - leftoverBits) {
            System.out.println("Incompatible bitfield lengths");
            return false;
        }

        Iterator<Boolean> it1 = bitfield.iterator();
        Iterator<Boolean> it2 = this.iterator();
        // Iterate through both bitfields and check if this bf has a missing piece
        int ind = 0;
        while (it1.hasNext() && ind < data.length*8 - leftoverBits) {
            ind++;
            if (!it1.next() && it2.next()) {
                return true;
            }
        }
        return false;
    }

    // Iterate through bitfield
    public Iterator<Boolean> iterator() {
        return new Iterator<Boolean>() {
            private int index = 0;
            private int bitIndex = 0;

            public boolean hasNext() {
                return index < data.length*8-leftoverBits;
            }

            public Boolean next() {
                boolean result = checkBit(index);
                index++;
                bitIndex++;
                if (bitIndex == 8) {
                    bitIndex = 0;
                }
                return result;
            }
        };
    }

}

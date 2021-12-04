/*
    Message Template for actual message protocols

    Parent for message types:
    * Choke
    * Unchoke
    * Interested
    * Not Interested
    * Have
    * Bitfield
    * Request
    * Piece

    Structure:
    * 4-byte Message Length
    * 1-byte message type
    * Variable size message payload
*/

package main.peer.message;

import java.io.ByteArrayOutputStream;

import main.Utils;

public class Message {

    // Define static types
    public static final byte CHOKE = 0;
    public static final byte UNCHOKE = 1;
    public static final byte INTERESTED = 2;
    public static final byte NOTINTERESTED = 3;
    public static final byte HAVE = 4;
    public static final byte BITFIELD = 5;
    public static final byte REQUEST = 6;
    public static final byte PIECE = 7;

    // Message structure
    private int length;
    private byte type;
    private byte[] payload;

    // Constructor
    public Message(byte type_, byte[] payload_) {
        // Check length of payload + add type byte, length of "this.length" not included
        if (payload_ == null) {
            this.length = 1;
        } else {
            this.length = 1 + payload_.length;
        }
        this.type = type_;
        this.payload = payload_;
    }

    public Message(Message m) {
        this.length = m.length;
        this.type = m.type;
        this.payload = m.payload;
    }

    public byte getType() {
        return this.type;
    }

    
    public int getLength() {
        return this.length;
    }
    
    public byte[] getPayload() {
        return this.payload;
    }
    
    protected void setPayload(byte[] b) {
        this.payload = b;
        this.length = 1 + b.length;
    }
    
    public byte[] getMessage() {
        if (this.length == 0) {
            return new byte[]{};
        }
        ByteArrayOutputStream b_out = new ByteArrayOutputStream(this.length+4);
        b_out.writeBytes(Utils.intToByteArray(this.length));
        b_out.write(this.type);
        if (this.payload != null) {
            b_out.write(this.payload, 0, this.payload.length);
        }

        return b_out.toByteArray();
    }
    
}

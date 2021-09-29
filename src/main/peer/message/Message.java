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
    private byte length;
    private byte type;
    private byte[] payload;

    // Constructor
    public Message(byte length_, byte type_, byte[] payload_) {
        this.length = length_;
        this.type = type_;
        this.payload = payload_;
    }

    public byte getType() {
        return this.type;
    }

    public byte getLength() {
        return this.length;
    }

    public byte[] getPayload() {
        return this.payload;
    }
    
}

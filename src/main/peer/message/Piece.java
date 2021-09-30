package main.peer.message;

public class Piece extends Message {
    
    public Piece(int index, byte[] content) {
        super((byte) content.length, Message.PIECE, content);
    }
}

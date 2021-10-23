package main.peer.message;

public class Piece extends Message {
    
    public Piece(int index, byte[] content) {
        super(Message.PIECE, content);
    }
}

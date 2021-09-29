package main.peer.message;

public class Unchoke extends Message {
    
    public Unchoke() {
        super((byte) 0, Message.UNCHOKE, null);
    }
}

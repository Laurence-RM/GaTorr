package main.peer.message;

public class Choke extends Message {
    
    public Choke() {
        super((byte) 0, Message.CHOKE, null);
    }
}

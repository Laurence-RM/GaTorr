package main.peer.message;

public class NotInterested extends Message {
    
    public NotInterested() {
        super((byte) 0, Message.NOTINTERESTED, null);
    }
}

package main.peer.message;

import main.Utils;

public class Request extends Message {
    
    public Request(int index) {
        super(Message.REQUEST, Utils.intToByteArray(index));
    }
}

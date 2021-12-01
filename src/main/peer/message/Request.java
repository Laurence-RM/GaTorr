package main.peer.message;

import main.Utils;

public class Request extends Message {
    
    private int index;

    public Request(int index) {
        super(Message.REQUEST, Utils.intToByteArray(index));
        this.index = index;
    }

    public Request(Message msg) {
        super(msg);
        this.index = Utils.byteArrayToInt(msg.getPayload());
    }

    public int getIndex() {
        return this.index;
    }
}

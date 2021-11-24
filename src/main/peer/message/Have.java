package main.peer.message;

import main.Utils;

public class Have extends Message {
    
    private int index;

    public Have(int index_) {
        super(Message.HAVE, Utils.intToByteArray(index_));
        this.index = index_;
    }

    public Have(Message msg) {
        super(msg);
        this.index = Utils.byteArrayToInt(msg.getPayload());
    }

    public int getIndex() {
        return this.index;
    }
}

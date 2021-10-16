package main.peer.message;

import main.Utils;

public class Have extends Message {
    
    private int index;

    public Have(int index_) {
        super((byte) 4, Message.HAVE, Utils.intToByteArray(index_));
        this.index = index_;
    }

    public int getIndex() {
        return this.index;
    }
}
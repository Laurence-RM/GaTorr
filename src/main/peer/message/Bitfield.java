package main.peer.message;

import main.peer.BitfieldObj;

public class Bitfield extends Message {
    
    public Bitfield(BitfieldObj bf) {
        super(Message.BITFIELD, bf.getData());
    }
}

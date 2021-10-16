package main.peer.message;

import main.peer.BitfieldObj;

public class Bitfield extends Message {
    
    // TODO: store bitfield in object
    public Bitfield(byte[] bf) {
        super((byte) bf.length, Message.BITFIELD, bf);
    }
}

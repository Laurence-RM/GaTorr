package main.peer.message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import main.Utils;

public class Piece extends Message {

    private int index;
    private byte[] content;
    
    public Piece(int index, byte[] content) {
        super(Message.PIECE, null);
        this.index = index;
        ByteArrayOutputStream baos = new ByteArrayOutputStream(4+content.length);
        baos.write(index);
        baos.write(content, 0, content.length);
        this.setPayload(baos.toByteArray());
    }

    public Piece(Message msg) {
        super(msg);
        ByteArrayInputStream bais = new ByteArrayInputStream(msg.getPayload());
        try {
            this.index = Utils.byteArrayToInt(bais.readNBytes(4));
            bais.readNBytes(this.content, 0, bais.available());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getIndex() {
        return this.index;
    }

    public byte[] getContent() {
        return this.content;
    }
    
}
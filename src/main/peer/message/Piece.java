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
        this.content = content;
        ByteArrayOutputStream baos = new ByteArrayOutputStream(4+content.length);
        baos.write(Utils.intToByteArray(index), 0, 4);
        baos.write(content, 0, content.length);
        this.setPayload(baos.toByteArray());
    }

    public Piece(Message msg) {
        super(msg);
        this.content = new byte[msg.getLength()-5];
        ByteArrayInputStream bais = new ByteArrayInputStream(msg.getPayload());
        try {
            this.index = Utils.byteArrayToInt(bais.readNBytes(4));
            if (bais.available() == msg.getLength()-5) {
                bais.readNBytes(this.content, 0, bais.available());
            }
            else {
                throw new IOException("Invalid payload length");
            }
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

    public void printContent() {
        for (int j = 0; j < content.length; j++) {
            System.out.format("%02X ", content[j]);
        }
        System.out.println();
    }
    
}
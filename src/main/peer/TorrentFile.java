package main.peer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import main.peer.message.Piece;

public class TorrentFile {
    public class PieceObj {
        private int index;
        private byte[] data;

        public PieceObj(int index, byte[] data) {
            this.index = index;
            this.data = data;
        }

        public PieceObj(Piece piece) {
            this.index = piece.getIndex();
            this.data = piece.getContent();
        }

        public int getIndex() {
            return index;
        }

        public byte[] getData() {
            return data;
        }

        public Piece getPieceMsg() {
            return new Piece(index, data);
        }

        // Testing
        public void printContent() {
            System.out.println("Piece " + index + ", " + data.length + " bytes:");
            for (int i = 0; i < 20; i++) {
                System.out.print(data[i] + " ");
            }
            System.out.println();
        }
    }

    private String fileName;
    private int fileSize;
    private int pieceSize;
    private int pieceCount;
    private int lastPieceSize;
    private boolean fileComplete;

    private File file;
    private int parCount;
    private BitfieldObj bitfield;

    public TorrentFile(String fileName, int fileSize, int pieceSize, File file_, BitfieldObj bitfield_) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.pieceSize = pieceSize;
        this.pieceCount = (int) Math.ceil((float) fileSize / pieceSize);
        this.lastPieceSize = fileSize % pieceSize;
        this.file = file_;
        this.fileComplete = file_.length() == fileSize;
        this.bitfield = bitfield_;
        this.parCount = 0;
        if (!fileComplete) {
            file.delete();
        }
    }

    public TorrentFile(String fileName, int fileSize, int pieceSize, File file_) {
        this(fileName, fileSize, pieceSize, file_, null);
    }


    public boolean isComplete() {
        return fileComplete;
    }

    public File getFile() {
        return file;
    }

    protected PieceObj getPieceFromFile(int index) {
        // Check file is complete
        if (!fileComplete) {
            return null;
        }

        int begin = index * pieceSize;
        int length = pieceSize;
        if (index == pieceCount - 1) {
            length = lastPieceSize;
        }
        byte[] data = new byte[length];

        // Read bytes from file
        try {
            FileInputStream fis = new FileInputStream(file);
            fis.skip(begin);
            fis.read(data);
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new PieceObj(index, data);
    }

    protected PieceObj getPieceFromPar(int index) {
        if (bitfield.checkBit(index)) {
            try {
                FileInputStream fis = new FileInputStream(file.getPath()+"_"+index+".par");
                int size = pieceSize;
                if (index == pieceCount - 1) {
                    size = lastPieceSize;
                }
                byte[] data = new byte[size];
                fis.read(data);
                fis.close();
                return new PieceObj(index, data);
            }
            catch (FileNotFoundException e) {
                System.out.println("Par file for Piece " + index + " not found");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public PieceObj getPiece(int index) {
        if (fileComplete) {
            return getPieceFromFile(index);
        } else {
            return getPieceFromPar(index);
        }
    }
    
    public void writePieceToFile(PieceObj pieceObj) {
        // Check file is complete
        if (fileComplete) {
            return;
        }
        
        // Write bytes to par file
        try {
            FileOutputStream fos = new FileOutputStream(file.getPath()+"_"+pieceObj.index+".par", true);
            fos.write(pieceObj.getData());
            fos.close();
            this.parCount++;
            updateBitfield(pieceObj.getIndex());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void setBitfield(BitfieldObj bitfield_) {
        this.bitfield = bitfield_;
    }

    private void updateBitfield(int index) {
        if (bitfield == null) {
            throw new RuntimeException("Bitfield reference in torrent file is null");
        }
        this.bitfield.setBit(index);
        if (parCount == pieceCount && bitfield.isComplete()) {
            combineParFiles();
        }
    }
    
    public void combineParFiles() {
        if (fileComplete) {
            return;
        }
        
        try {
            FileOutputStream fos = new FileOutputStream(file);
            for (int i = 0; i < pieceCount; i++) {
                File f = new File(file.getPath()+"_"+i+".par");
                FileInputStream fis = new FileInputStream(f);
                int size = pieceSize;
                if (i == pieceCount - 1) {
                    size = lastPieceSize;
                }
                byte[] data = new byte[size];
                fis.read(data);
                fos.write(data);
                fis.close();
                f.delete();
            }
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.fileComplete = true;
    }
    
        public static void main(String args[]) {
            File file = new File("peer_1001/thefile");
            TorrentFile torrentFile = new TorrentFile("thefile", 2167705, 16384, file);
            torrentFile.setBitfield(new BitfieldObj(torrentFile.pieceCount));
            
            File file2 = new File("peer_1002/thefile");
            TorrentFile tf2 = new TorrentFile("thefile", 2167705, 16384, file2);
            tf2.setBitfield(new BitfieldObj(torrentFile.pieceCount));

            PieceObj pieceObj = torrentFile.getPiece(2);
            tf2.writePieceToFile(pieceObj);
            PieceObj po = tf2.getPiece(2);
            po.printContent();
        }
}

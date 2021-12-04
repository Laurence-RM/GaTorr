package main.peer;

import java.io.File;
import java.io.FileInputStream;
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

    public TorrentFile(String fileName, int fileSize, int pieceSize, File file_) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.pieceSize = pieceSize;
        this.pieceCount = (int) Math.ceil((double) fileSize / pieceSize);
        this.lastPieceSize = fileSize % pieceSize;
        this.file = file_;
        this.fileComplete = file_.length() == fileSize;
        if (!fileComplete) {
            file.delete();
        }
    }

    public boolean isComplete() {
        return fileComplete;
    }

    public File getFile() {
        return file;
    }

    public PieceObj getPieceFromFile(int index) {
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    
        public static void main(String args[]) {
            File file = new File("peer_1001/thefile");
            TorrentFile torrentFile = new TorrentFile("thefile", 2167705, 16384, file);
            Piece p = torrentFile.getPieceFromFile(4).getPieceMsg();

            File file2 = new File("peer_1002/thefile");
            TorrentFile tf2 = new TorrentFile("thefile", 2167705, 16384, file2);
            PieceObj pObj = tf2.new PieceObj(p);
            tf2.writePieceToFile(pObj);
        }
}

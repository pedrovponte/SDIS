import java.util.concurrent.*;
import java.util.Arrays;
import java.io.*;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;

public class PutChunkMessageThread implements Runnable {
    private byte[] message;
    private Peer peer;
    private byte[] header;
    private byte[] body;
    private int senderId;
    private String fileId;
    private int chunkNo;
    private int replication_degree;
    private String protocolVersion;


    // <Version> PUTCHUNK <SenderId> <FileId> <ChunkNo> <ReplicationDeg> <CRLF><CRLF><Body>
    public PutChunkMessageThread(byte[] message, Peer peer) {
        this.message = message;
        this.peer = peer;
        splitHeaderAndBody();
        String[] headerStr = new String(this.header).split(" ");
        this.protocolVersion = headerStr[0];
        this.senderId = Integer.parseInt(headerStr[2]);
        this.fileId = headerStr[3];
        this.chunkNo = Integer.parseInt(headerStr[4]);
        this.replication_degree = Integer.parseInt(headerStr[5]);
        // System.out.println("SenderId: " + this.senderId);
        // System.out.println("FileId: " + this.fileId);
        // System.out.println("ChunkNo: " + this.chunkNo);
        // System.out.println("Replication: " + this.replication_degree);

    }


    // thread that receives a PUTCHUNK message, checks if already has backed up the given chunk, checks if it has space available to store the chunk and then stores the chunk
    // depending on the protocol version, it can check if the replication degree has already been achieved or not
    // finally, sends a STORED message
    @Override
    public void run() {
        // in case senderId and peerId are equal, the thread returns because a peer must never store the chunks of its own files.
        if(checkIfSelf() == 1) {
            return;
        }

        System.out.println("RECEIVED: " + this.protocolVersion + " PUTCHUNK " + this.senderId + " " + this.fileId + " " + this.chunkNo + " " + this.replication_degree);
        System.out.println();

        //check if the peer already has stored this chunk
        if(this.peer.getStorage().hasChunk(this.fileId, this.chunkNo) == true) {
            System.out.println("Already has chunk");
            System.out.println();
            Random r = new Random();
            int low = 0;
            int high = 400;
            int result = r.nextInt(high-low) + low;
            // <Version> STORED <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
            String toSend = this.peer.getProtocolVersion() + " STORED " + this.peer.getPeerId() + " " + this.fileId + " " + this.chunkNo + " " + "\r\n\r\n";
            this.peer.getThreadExec().schedule(new ThreadSendMessages(this.peer.getMC(), toSend.getBytes()), result, TimeUnit.MILLISECONDS);
            System.out.println("SENT: " + toSend);
            return;
        }

        ArrayList<FileManager> files = this.peer.getStorage().getFilesStored();

        // checks if this chunk is part of a file that this peer has backed up. It is for REMOVED messages because in this case the senderId is different from the initiator peerId
        for(int i = 0; i < files.size(); i++) {
            if(files.get(i).getFileID().equals(this.fileId)) {
                System.out.println("Initiator peer of this file (" + files.get(i).getPath() + "). Can't store chunks of this one.");
                System.out.println();
                return;
            }
        }

        // in this version, it has to check if the wished replication degree had already been achieved before
        // to do that, first it waits a random time between 0 and 1s before check the replication degree
        if(protocolVersion.equals("2.0")){
            String chunkId = this.fileId + "_" + this.chunkNo;
            int storedReplicationsAfter = 0;

            Random r = new Random();
            int low = 0;
            int high = 1000;
            int result = r.nextInt(high-low) + low;

            /*try {
                Thread.sleep(this.peer.getPeerId() * 123 % 1000);
            } catch(InterruptedException e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            }*/

            ConcurrentHashMap<Integer, ArrayList<String>> distribution = this.peer.getStorage().getChunksDistribution();

            for(Integer key : distribution.keySet()) {
                if(distribution.get(key).contains(chunkId)) {
                    storedReplicationsAfter++;
                }
            }

            // in this case the replication is achieved
            if (storedReplicationsAfter >= this.replication_degree){
                System.out.println("Replication degree already satisfied");
                System.out.println();
                return;
            }
        }

        // checks if the peer has free space to save the chunk
        if(!(this.peer.getStorage().checkIfHasSpace(this.body.length))) {
            System.out.println("Doesn't have space to store chunk " + this.chunkNo);
            System.out.println();
            return;
        }

        Chunk chunk = new Chunk(this.fileId, this.chunkNo, this.body, this.replication_degree, this.body.length);

        this.peer.getStorage().addChunk(chunk);

        // create the chunk file in the peer directory
        String dir = "peer_" + this.peer.getPeerId();
        String backupDir = "peer_" + this.peer.getPeerId() + "/" + "backup";
        String file = "peer_" + this.peer.getPeerId() + "/" + "backup" + "/" + this.fileId + "_" + this.chunkNo;
        File directory = new File(dir);
        File backupDirectory = new File(backupDir);
        File f = new File(file);

        try{
            if (!directory.exists()){
                directory.mkdir();
                backupDirectory.mkdir();
                f.createNewFile();
            } 
            else {
                if (directory.exists()) {
                    if(backupDirectory.exists()) {
                        f.createNewFile();
                    }
                    else {
                        backupDirectory.mkdir();
                        f.createNewFile();
                    }
                } 
            }

            FileOutputStream fos = new FileOutputStream(f);
            fos.write(body);
            fos.close();

        } catch(Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
        
        // <Version> STORED <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
        String toSend = this.peer.getProtocolVersion() + " STORED " + this.peer.getPeerId() + " " + this.fileId + " " + this.chunkNo + " " + "\r\n\r\n";
        //this.peer.getThreadExec().schedule(new ThreadSendMessages(this.peer.getMC(), toSend.getBytes()), result, TimeUnit.MILLISECONDS);
        this.peer.getThreadExec().execute(new ThreadSendMessages(this.peer.getMC(), toSend.getBytes()));
        System.out.println("SENT: " + toSend);
        System.out.println();
    }

    public void splitHeaderAndBody() {
        int i;
        for(i = 0; i < this.message.length; i++) {
            if(this.message[i] == 0xD && this.message[i + 1] == 0xA && this.message[i + 2] == 0xD && this.message[i + 3] == 0xA) {
                break;
            }
        }

        this.header = Arrays.copyOfRange(this.message, 0, i);
        this.body = Arrays.copyOfRange(this.message, i + 4, message.length); // i+4 because between i and i+4 are \r\n\r\n
    }

    // checks if the senderId is equal to the receiver peerId. In case it is equal, returns 1, else returns 0.
    int checkIfSelf() {
        if(this.peer.getPeerId() == this.senderId) {
            return 1;
        }
        return 0;
    }
    
}

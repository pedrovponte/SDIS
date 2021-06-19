import java.util.concurrent.*;
import java.util.Random;
import java.util.ArrayList;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public class RemovedMessageThread implements Runnable {

    private byte[] message;
    private Peer peer;


    public RemovedMessageThread(byte[] message, Peer peer) {
        this.message = message;
        this.peer = peer;
    }


    // thread that receives REMOVED message and checks if this peer has the chunk stored. Then it will check if the current chunk replication is correct or not
    // if not, then the peer will wait a random time between 0 and 400 ms to check if another peer has backed up the chunk replication
    // in case not, the this peer will send a PUTCHUNK message in order to try to achieve the correct replication
    @Override
    public void run() {
        // <Version> REMOVED <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
        String[] messageStr = (new String(this.message)).split(" ");
        String protocolVersion = messageStr[0];
        int senderId = Integer.parseInt(messageStr[2]);
        String fileId = messageStr[3];
        int chunkNo = Integer.parseInt(messageStr[4]);

        System.out.println("RECEIVED: " + protocolVersion + " REMOVED " + senderId + " " + fileId + " " + chunkNo);
        System.out.println();

        this.peer.getStorage().deleteSpecificChunksDistribution(fileId, chunkNo, senderId);

        // checks if the senderId is equal to the receiver peerId
        if(this.peer.getPeerId() == senderId) {
            return;
        }

        if(!(this.peer.getStorage().hasChunk(fileId, chunkNo))) {
            System.out.println("Doesn't have chunk stored");
            return;
        }

        ConcurrentHashMap<Integer, ArrayList<String>> distribution = this.peer.getStorage().getChunksDistribution();
        String chunkId = fileId + "_" + chunkNo;
        int storedReplicationsBefore = 0;

        // System.out.println("-----REGISTS COUNT STORED BEFORE-------------");
        // for(Integer key : distribution.keySet()) {
        //     System.out.println(key + ": " + distribution.get(key));  
        // }
        // System.out.println("--------------------------");


        for(Integer key : distribution.keySet()) {
            if(distribution.get(key).contains(chunkId)) {
                storedReplicationsBefore++;
            }
        }

        Chunk chunk = this.peer.getStorage().getChunksStored().get(chunkId);

        if(chunk.getReplication() <= storedReplicationsBefore) {
            System.out.println("Correct replication. Doesn't need to replicate.");
            return;
        }

        Random r = new Random();
        int low = 0;
        int high = 1000;
        int result = r.nextInt(high-low) + low;

        try {
            Thread.sleep(result);
        } catch(InterruptedException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }

        int storedReplicationsAfter = 0;

        // System.out.println("-----REGISTS COUNT STORED AFTER-------------");
        // for(Integer key : distribution.keySet()) {
        //     System.out.println(key + ": " + distribution.get(key));  
        // }
        // System.out.println("--------------------------");


        for(Integer key : distribution.keySet()) {
            if(distribution.get(key).contains(chunkId)) {
                storedReplicationsAfter++;
            }
        }

        // if the replication before and after are different, then any peer had already saved the chunk
        if(storedReplicationsBefore != storedReplicationsAfter) {
            System.out.println("Another have already replicate chunk.");
            return;
        }

        // <Version> PUTCHUNK <SenderId> <FileId> <ChunkNo> <ReplicationDeg> <CRLF><CRLF><Body>
        String header = protocolVersion + " PUTCHUNK " + this.peer.getPeerId() + " " + fileId  + " " + chunkNo + " " + chunk.getReplication() + " \r\n\r\n";
            
        try {
            byte[] headerBytes = header.getBytes(StandardCharsets.US_ASCII);
            byte[] body = chunk.getChunkMessage();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
            outputStream.write(headerBytes);
            outputStream.write(body);
            byte[] message = outputStream.toByteArray();

            // send threads
            this.peer.getThreadExec().execute(new ThreadSendMessages(this.peer.getMDB(), message));
            System.out.println("SENT: "+ header);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }     
    }   
}

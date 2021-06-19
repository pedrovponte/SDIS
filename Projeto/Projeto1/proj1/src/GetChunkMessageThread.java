import java.util.Arrays;
import java.io.*;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.*;
import java.nio.charset.StandardCharsets;
import java.net.*;

public class GetChunkMessageThread implements Runnable {
    private byte[] message;
    private Peer peer;


    public GetChunkMessageThread(byte[] message, Peer peer) {
        this.message = message;
        this.peer = peer;
    }


    // receives GETCHUNK message, checks if the peer has stored the wanted chunk and if it has, then checks if another peer had already sent that chunk to the initiator peer
    // in case not, then send it to the initiator peer in case another
    // depending on protocol version, it sends it using multicast channel ("1.0") or TCP channel ("2.0") 
    @Override
    public void run() {
        // <Version> GETCHUNK <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
        // <Version> GETCHUNK <SenderId> <FileId> <ChunkNo> <Port> <CRLF><CRLF>
        String[] messageStr = (new String(this.message)).split(" ");
        String protocolVersion = messageStr[0];
        int senderId = Integer.parseInt(messageStr[2]);
        String fileId = messageStr[3];
        int chunkNo = Integer.parseInt(messageStr[4]);
        int port = 0;

        if(protocolVersion.equals("2.0")) {
            port = Integer.parseInt(messageStr[5]);
        }   

        // checks if the senderId is equal to the receiver peerId
        if(this.peer.getPeerId() == senderId) {
            return;
        }

        System.out.println("RECEIVED: " + protocolVersion + " GETCHUNK " + senderId + " " + fileId + " " + chunkNo);

        ConcurrentHashMap<String, Chunk> chunksStored = this.peer.getStorage().getChunksStored();
        String chunkId = fileId + "_" + chunkNo;

        // checks if this peer has the chunk stored
        if(!(chunksStored.containsKey(chunkId))){
            System.out.println("Don't have chunk " + chunkNo + " stored");
            System.out.println();
            return;
        }

        // To avoid flooding the host with CHUNK messages, each peer shall wait for a random time uniformly distributed 
        // between 0 and 400 ms, before sending the CHUNK message. If it receives a CHUNK message before that time expires, 
        // it will not send the CHUNK message.
        int low = 0;
        int high = 0;
        int result = 0;
        Random r = new Random();
        
        if(protocolVersion.equals("1.0")){
            low = 0;
            high = 400;
            result = r.nextInt(high-low) + low;
        }    
        else {
            low = 0;
            high = 1000;
            result = r.nextInt(high-low) + low;
        }

        // initial chunk messages received for chunkId
        Integer initialNumber = this.peer.getReceivedChunkMessages().get(chunkId);

        try {
            Thread.sleep(result);
        } catch(InterruptedException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
        
        // final chunk messages received for chunkId
        Integer finalNumber = this.peer.getReceivedChunkMessages().get(chunkId);

        // in case the messages number is different, then another peer already has sent that chunk to the initiator peer, so this doesn't need to send again
        if(initialNumber != finalNumber) {
            System.out.println("A peer already has sent chunk " + chunkNo);
            System.out.println();
            return;
        }

        // <Version> CHUNK <SenderId> <FileId> <ChunkNo> <CRLF><CRLF><Body>
        String header = protocolVersion + " CHUNK " + this.peer.getPeerId() + " " + fileId + " " + chunkNo + " \r\n\r\n";

        try {
            byte[] headerBytes = header.getBytes(StandardCharsets.US_ASCII);
            byte[] body = chunksStored.get(chunkId).getChunkMessage();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
            outputStream.write(headerBytes);
            outputStream.write(body);
            byte[] message = outputStream.toByteArray();

            // using MDR multicast channel
            if(protocolVersion.equals("1.0")) {
                this.peer.getThreadExec().execute(new ThreadSendMessages(this.peer.getMDR(), message));
            }
            // using TCP
            else {
                this.peer.getThreadExec().execute(new ThreadChunkMessage(message, port));
            }
            System.out.println("SENT: " + header);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        } 
    }
}

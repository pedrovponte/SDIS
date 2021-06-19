import java.util.concurrent.*;
import java.io.File;

public class DeleteMessageThread implements Runnable {
    private byte[] message;
    private Peer peer;


    public DeleteMessageThread(byte[] message, Peer peer) {
        this.message = message;
        this.peer = peer;
    }

    // Thread that receives the DELETE message from the initiator peer, in order to delete all the chunks of the file fileId
    @Override
    public void run() {
        String[] messageStr = (new String(this.message)).split(" ");
        
        // <Version> DELETE <SenderId> <FileId> <CRLF><CRLF>

        int senderId = Integer.parseInt(messageStr[2]);
        String fileId = messageStr[3];
        String version = messageStr[0];

        // if the senderId equals to this peer id, it doesn't have chunks of the file fileId, because it is the initiator peer
        if(senderId == this.peer.getPeerId()) {
            return;
        }

        ConcurrentHashMap<String, Chunk> chunks = this.peer.getStorage().getChunksStored();

        // iterate the chunks table of this peer and delete all of them that belong to the deleted file
        for(String key : chunks.keySet()) {
            Chunk chunk = chunks.get(key);
            if(chunk.getFileId().equals(fileId)) {
                this.peer.getStorage().deleteChunk(key);
                File filename = new File("peer_" + this.peer.getPeerId() + "/backup/" + key);
                filename.delete();
                System.out.println("RECEIVED: " + new String(this.message));
                System.out.println();
            }
        }

        if(version.equals("1.0")) {
            this.peer.getStorage().deleteChunksDistribution(fileId);
        }

        // send DELETED message (delete enhancement)
        if(version.equals("2.0")) {
            this.peer.getStorage().deleteChunksDistribution(fileId, this.peer.getPeerId());
            // <Version> DELETED <SenderId> <InitiatorId> <FileId> <CRLF><CRLF>
            String toSend = this.peer.getProtocolVersion() + " DELETED " + this.peer.getPeerId() + " " + senderId + " " + fileId + " \r\n\r\n";
            System.out.println();
            this.peer.getThreadExec().execute(new ThreadSendMessages(this.peer.getMC(), toSend.getBytes()));
            System.out.println("SENT: " + toSend);
            System.out.println();
        }
    }
}

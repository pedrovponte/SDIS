import java.util.Random;
import java.util.concurrent.TimeUnit;

public class ManageReceivedMessages implements Runnable {

    private Peer peer;
    private byte[] message;

    public ManageReceivedMessages(Peer peer, byte[] message) {
        this.peer = peer; //returns a copy of this string with leading and trailing white space removed
        this.message = message;
    }

    
    // checks the message type and then creates a new thread to treat that message
    public void run() {
        // message: <Version> <MessageType> <SenderId> <FileId> <ChunkNo> <ReplicationDeg> <CRLF>
        String[] messageStr = new String(this.message).split(" ");
        // System.out.println("Manager message: " + messageStr);
        switch (messageStr[1]){
            case "PUTCHUNK":
                Random r = new Random();
                int low = 0;
                int high = 400;
                int result = r.nextInt(high-low) + low;
                this.peer.getThreadExec().schedule(new PutChunkMessageThread(this.message, this.peer), result, TimeUnit.MILLISECONDS);
                break;

            case "STORED":
                this.peer.getThreadExec().execute(new StoredMessageThread(this.message, this.peer));
                break;

            case "DELETE":
                this.peer.getThreadExec().execute(new DeleteMessageThread(this.message, this.peer));
                break;

            case "GETCHUNK":
                this.peer.getThreadExec().execute(new GetChunkMessageThread(this.message, this.peer));
                break;

            case "CHUNK":
                this.peer.getThreadExec().execute(new ChunkMessageThread(this.message, this.peer));
                break;

            case "REMOVED":
                this.peer.getThreadExec().execute(new RemovedMessageThread(this.message, this.peer));
                break;

            case "DELETED":
                this.peer.getThreadExec().execute(new DeletedMessageThread(this.message, this.peer));
                break;

            case "WORKING":
                this.peer.getThreadExec().execute(new WorkingMessageThread(this.message, this.peer));
                break;
            
            case "CHUNKTCP":
                this.peer.getThreadExec().execute(new ChunkTCPMessageThread(this.message, this.peer));
                break;
                
            default:
                break;
        }

    }
    
}

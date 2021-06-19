import java.util.concurrent.TimeUnit;
import java.util.concurrent.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.*;

public class ThreadCountStored implements Runnable {
    private Peer peer;
    private int replication;
    private String fileId;
    private int chunkNo;
    private ChannelController channel;
    private byte[] message;
    private int tries = 0;
    private int time = 1;


    public ThreadCountStored(Peer peer, int replication, String fileId, int chunkNo, ChannelController channel, byte[] message) {
        this.peer = peer;
        this.replication = replication;
        this.fileId = fileId;
        this.chunkNo = chunkNo;
        this.channel = channel;
        this.message = message;
    }


    // thread that checks if the replication degree of a chunk has already been achieved
    @Override
    public void run() {
        // <Version> STORED <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>

        ConcurrentHashMap<Integer, ArrayList<String>> distribution = this.peer.getStorage().getChunksDistribution();
        String chunkId = this.fileId + "_" + this.chunkNo;
        int storedReplications = 0;

        // System.out.println("-----REGISTS COUNT STORED-------------");
        // for(Integer key : distribution.keySet()) {
        //     System.out.println(key + ": " + distribution.get(key));  
        // }
        // System.out.println("--------------------------");


        for(Integer key : distribution.keySet()) {
            if(distribution.get(key).contains(chunkId)) {
                storedReplications++;
            }
        }

        // System.out.println("Stored replications of " + chunkId + ": " + storedReplications);
        
        // in case the wished replication degree not yet achieved and the number of tries is lower than 4, then it will send the PUTCHUNK message again in order to try to achieve the wated replication
        if(storedReplications < this.replication && this.tries < 4) {
            this.peer.getThreadExec().execute(new ThreadSendMessages(this.channel, this.message));
            String[] messageArr = (new String(this.message).toString()).split(" ");
            System.out.println("SENT: "+ messageArr[0] + " " + messageArr[1] + " " + messageArr[2] + " " + messageArr[3] + " " + messageArr[4]);
            this.time = this.time * 2;
            // System.out.println("TIME: " + this.time);
            this.tries++;
            this.peer.getThreadExec().schedule(this, this.time, TimeUnit.SECONDS);
            // System.out.println("After create thread");
        }

        if(this.tries >= 4) {
            System.out.println("Minimum replication not achieved");
            return;
        }
        else if(storedReplications >= this.replication) {
            System.out.println("Replication completed: " + storedReplications);
            System.out.println();
            return;
        }
    }
}

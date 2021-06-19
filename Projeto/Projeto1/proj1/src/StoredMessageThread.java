import java.util.Arrays;

public class StoredMessageThread implements Runnable {
    private byte[] message;
    private Peer peer;
    private byte[] header;

    
    public StoredMessageThread(byte[] message, Peer peer) {
        this.message = message;
        this.peer = peer;
    }


    // thread that receives STORED message
    @Override
    public void run() {
        // <Version> STORED <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
        splitHeader();
        String[] messageStr = new String(this.header).split(" ");
        // System.out.println("Message: " + messageStr[0] + " " + messageStr[1] + " " + messageStr[2] + " " + messageStr[3] + " " + messageStr[4]);
        String version = messageStr[0];
        int senderId = Integer.parseInt(messageStr[2]);
        String fileId = messageStr[3];
        int chunkNo = Integer.parseInt(messageStr[4]);

        // System.out.println("Version: " + version);
        // System.out.println("Sender: " + senderId);
        // System.out.println("File: " + fileId);
        // System.out.println("ChunkNo: " + chunkNo);

        System.out.println("RECEIVED: " + new String(this.message));

        // add the regist to the chunksDistribution table. By doing that, all peers know the peers that each one has stored
        this.peer.getStorage().addChunksDistribution(senderId, fileId, chunkNo);

        if(this.peer.getPeerId() != senderId) {
            //System.out.println("Different peer and sender");
            if(this.peer.getStorage().hasRegisterStore(fileId, chunkNo)) {
                //System.out.println("Has regist");
                this.peer.getStorage().incrementStoredMessagesReceived(senderId, fileId, chunkNo);
            }
        }
    }

    public void splitHeader() {
        int i;
        for(i = 0; i < this.message.length; i++) {
            if(this.message[i] == 0xD && this.message[i + 1] == 0xA && this.message[i + 2] == 0xD && this.message[i + 3] == 0xA) {
                break;
            }
        }

        this.header = Arrays.copyOfRange(this.message, 0, i);
    }
}

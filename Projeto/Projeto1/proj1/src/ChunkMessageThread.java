import java.util.concurrent.*;
import java.util.Arrays;
public class ChunkMessageThread implements Runnable{
    private byte[] message;
    private Peer peer;
    private byte[] header;
    private byte[] body;


    public ChunkMessageThread(byte[] message, Peer peer) {
        this.message = message;
        this.peer = peer;
        splitHeaderAndBody();
        
    }
    

    @Override
    public void run(){
        // <Version> CHUNK <SenderId> <FileId> <ChunkNo> <CRLF><CRLF><Body>
        String[] headerStr = new String(this.header).split(" ");
        String protocolVersion = headerStr[0];
        int senderId = Integer.parseInt(headerStr[2]);
        String fileId = headerStr[3];
        int chunkNo = Integer.parseInt(headerStr[4]);

        System.out.println("RECEIVED: " + protocolVersion + " CHUNK " + senderId + " " + fileId + " " + chunkNo);
        System.out.println();

        String chunkId = fileId + "_" + chunkNo;
        this.peer.incrementReceivedChunkMessagesNumber(chunkId);

        if(this.peer.getPeerId() != senderId) {
            if(this.peer.getStorage().hasFileToRestore(fileId) && !this.peer.getStorage().hasRegisterToRestore(chunkId)) {
                this.peer.getStorage().addChunkToRestore(chunkId, this.body);
            }
            else {
                System.out.println("Chunk " + chunkNo + " not requested or already have been restored");
                System.out.println();
            }
        }        
    }

    // split message into header and body using <CRLF> as reference to split them
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
}

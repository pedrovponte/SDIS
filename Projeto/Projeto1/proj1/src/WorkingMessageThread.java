import java.util.ArrayList;

public class WorkingMessageThread implements Runnable{
    private Peer peer;
    private String protocolVersion;
    private int senderId;


    // <Version> WORKING <PeerId> <CRLF><CRLF>
    public WorkingMessageThread(byte[] message, Peer peer) {
        this.peer = peer;
        String[] headerStr = new String(message).split(" ");
        this.protocolVersion = headerStr[0];
        this.senderId = Integer.parseInt(headerStr[2]);
    }


    // thread that receives the WORKING message and checks if this peer has any regist to send to the sender peer in order to make this one delete chunks that this one has stored
	@Override
	public void run() {
		if(this.peer.getPeerId() == senderId) {
            return;
        }

        if(!protocolVersion.equals("2.0")) {
            return;
        }
        
        System.out.println("RECEIVED: " + protocolVersion + " WORKING " + this.senderId);

        ArrayList<String> toDeleteFiles = this.peer.getStorage().getFilesToDelete(this.senderId);
        System.out.println("Files to delete: " + toDeleteFiles);

        for(String file : toDeleteFiles) {
            // <Version> DELETE <SenderId> <FileId> <CRLF><CRLF>
            for(int i = 0; i < 5; i++) {
                String toSend = protocolVersion + " DELETE " + this.peer.getPeerId() + " " + file + " \r\n\r\n";
                this.peer.getThreadExec().execute(new ThreadSendMessages(this.peer.getMC(), toSend.getBytes()));
                System.out.println("SENT: " + toSend);
                try {
                    Thread.sleep(200);
                } catch(InterruptedException e) {
                    System.err.println(e.getMessage());
                    e.printStackTrace();
                }
            }
        }
	}
}
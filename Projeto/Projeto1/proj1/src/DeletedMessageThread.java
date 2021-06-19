public class DeletedMessageThread implements Runnable {
    private Peer peer;
    private String protocolVersion;
    private int senderId;
    private int initiatorId;
    private String fileId;


    // <Version> DELETED <SenderId> <InitiatorId> <FileId> <CRLF><CRLF>
    public DeletedMessageThread(byte[] message, Peer peer) {
        this.peer = peer;
        String[] headerStr = new String(message).split(" ");
        this.protocolVersion = headerStr[0];
        this.senderId = Integer.parseInt(headerStr[2]);
        this.initiatorId = Integer.parseInt(headerStr[3]);
        this.fileId = headerStr[4];
    }


    // Thread that receives the message of type DELETED, in order to make that peer know that the peer with id senderId has deleted the chunks of the file with id fileId (part of delete enhancement).
	@Override
	public void run() {
        if(!this.protocolVersion.equals("2.0")) {
            return;
        }

        // deletes the regist of the pair (senderId, fileId) from chunksDistribution table of that peer
        this.peer.getStorage().deleteChunksDistribution(fileId, senderId);

		if(!(this.peer.getPeerId() == initiatorId)) {
            return;
        }
        
        System.out.println("RECEIVED: " + this.protocolVersion + " DELETED " + this.senderId + " " + this.initiatorId + " " + this.fileId);
        System.out.println();

        // adds the senderId to the list of this fileId in filesDeleted table in order to know that the peer with id senderId has already deleted the chunks of this file
        this.peer.getStorage().addDeletedFile(this.fileId, this.senderId);
	}
}
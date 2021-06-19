import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.io.*;
import java.net.*;

public class TCPChannel implements Runnable {
    //private String address;
    //private int port;
    private Peer peer;
    private ServerSocket serverSocket;
    private DataInputStream dis;
    private Socket socket;
    private byte[] header;
    private byte[] body;


    public TCPChannel(ServerSocket serverSocket, Peer peer) {
        this.serverSocket = serverSocket;
        this.peer = peer;
    }


    public ServerSocket getServerSocket() {
        return this.serverSocket;
    }


    // server socket to receive the messages sent by ThreadChunkMessages
    public void run() {
        while(!this.serverSocket.isClosed()) {
            try {
                this.socket = this.serverSocket.accept();
                this.dis = new DataInputStream(socket.getInputStream());
            } catch (IOException e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            }

            byte[] message = new byte[65000];
        
            try {
                this.dis.read(message);
            } catch(Exception e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            }

            splitHeaderAndBody(message);
            String[] headerStr = new String(this.header).split(" ");
            String protocolVersion = headerStr[0];
            int senderId = Integer.parseInt(headerStr[2]);
            String fileId = headerStr[3];
            int chunkNo = Integer.parseInt(headerStr[4]);

            System.out.println("RECEIVED: " + protocolVersion + " CHUNK " + senderId + " " + fileId + " " + chunkNo);
            // in order to make the other peers know that chunk senderId has sent the chunk chunkNo
            String toSend = protocolVersion + " CHUNKTCP " + senderId + " " + fileId + " " + chunkNo;
            this.peer.getThreadExec().execute(new ThreadSendMessages(this.peer.getMC(), toSend.getBytes()));

            String chunkId = fileId + "_" + chunkNo;

            if(this.peer.getPeerId() != senderId) {
                if(this.peer.getStorage().hasFileToRestore(fileId) && !this.peer.getStorage().hasRegisterToRestore(chunkId)) {
                    this.peer.getStorage().addChunkToRestore(chunkId, this.body);
                }
                else {
                    System.out.println("Chunk " + chunkNo + " not requested or already have been restored");
                }
            }        
        }        
    }
    

    public void splitHeaderAndBody(byte[] message) {
        int i;
        for(i = 0; i < message.length; i++) {
            if(message[i] == 0xD && message[i + 1] == 0xA && message[i + 2] == 0xD && message[i + 3] == 0xA) {
                break;
            }
        }

        this.header = Arrays.copyOfRange(message, 0, i);
        this.body = Arrays.copyOfRange(message, i + 4, message.length); // i+4 because between i and i+4 are \r\n\r\n
    }
    
}

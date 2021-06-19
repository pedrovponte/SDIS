import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.Arrays;

public class ChannelController implements Runnable {
    private InetAddress address;
    private int port;
    private Peer peer;


    public ChannelController(String address, int port, Peer peer) {
        try{
            this.address = InetAddress.getByName(address);
            this.port = port;
            this.peer = peer;
        } catch(UnknownHostException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        } 
    }


    public void sendMessage(byte[] message) {

        // use multicast socket or datagram socket?
        // Multicast is going to be MUCH more efficient than any form of unicasting, however, multicasting is not reliable,
        // and does not work across heterogeneous networks like the internet, where the operators tend to disable multicast traffic
        try(MulticastSocket multicastSocket = new MulticastSocket(this.port)) {
            DatagramPacket datagramPacket = new DatagramPacket(message, message.length, this.address, this.port);
            multicastSocket.send(datagramPacket);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }


    public void run() {

        // maximum size of a chunk is 64KBytes (body)
        // header has at least 32 bytes (fileId) + version + messageType + senderId + chunkNo + replicationDegree
        // so 65KBytes should be sufficient to receive the message
        byte[] buf = new byte[65000];

        try {
            MulticastSocket multicastSocket = new MulticastSocket(port);
            multicastSocket.joinGroup(address);

            // listens multicast channel
            while(true) {
                // receive a packet
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                multicastSocket.receive(packet);

                byte[] received = Arrays.copyOf(buf, packet.getLength());
                ManageReceivedMessages manager = new ManageReceivedMessages(this.peer, received);
                
                // call a thread to execute the task
                this.peer.getThreadExec().execute(manager);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }
}

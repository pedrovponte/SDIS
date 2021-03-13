import java.io.*;
import java.net.*;
    
public class Client{
    public static void main(String [] args) throws IOException{
        if(args.length < 4 || args.length > 5 || (args[2] == "REGISTER" && args.length != 5) || (args[2] == "LOOKUP" && args.length != 4) /*|| (args[2] != "REGISTER" && args[2] != "LOOKUP")*/) {
            System.out.println("Usage:");
            System.out.println("\t Client <mcast addr> <mcast_port> REGISTER <DNS name> <IP address>");
            System.out.println("\t Client <mcast_addr> <mcast_port> LOOKUP <DNS name>");
            return;
        }

        System.out.println("New request issued");

        InetAddress multAddress = InetAddress.getByName(args[0]);
        int multPort = Integer.parseInt(args[1]);
        MulticastSocket multicastSocket = new MulticastSocket(multPort);
        multicastSocket.joinGroup(multAddress);

        byte[] buf = new byte[256];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        multicastSocket.receive(packet);

        String[] response = new String(packet.getData()).split(" ");
        InetAddress dgramAddress = InetAddress.getByName(response[0]);
        int dgramPort = Integer.parseInt(response[1].trim());
        
        byte[] req = new byte[256];
        
        String reqString = "";

        for(int i = 2; i < args.length; i++) {
            reqString = reqString + args[i] + " ";
        }

        reqString.trim();
        System.out.println(reqString);
        req = reqString.getBytes();
        DatagramSocket socket = new DatagramSocket();
        
        DatagramPacket requestDatagram = new DatagramPacket(req, req.length, dgramAddress, dgramPort); 
        socket.send(requestDatagram);
        System.out.println("Request sent");

        byte[] rbuf = new byte[256];
        DatagramPacket responsePacket = new DatagramPacket(rbuf, rbuf.length);
        socket.receive(responsePacket);
        System.out.println("Response received");
        String responseReq = new String(responsePacket.getData(), 0, responsePacket.getLength());
        System.out.println("Client: " + reqString + ": " + responseReq);

        multicastSocket.leaveGroup(multAddress);
        socket.close();
        multicastSocket.close();
    }
}
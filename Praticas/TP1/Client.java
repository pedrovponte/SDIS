import java.io.*;
import java.net.*;
    
public class Client{
    public static void main(String [] args) throws IOException{
        if(args.length < 4 || args.length > 5 || (args[2] == "REGISTER" && args.length != 5) || (args[2] == "LOOKUP" && args.length != 4) /*|| (args[2] != "REGISTER" && args[2] != "LOOKUP")*/) {
            System.out.println("Usage:");
            System.out.println("\t Client <host> <port> REGISTER <DNS name> <IP address>");
            System.out.println("\t Client <host> <port> LOOKUP <DNS name>");
            return;
        }

        System.out.println("New request issued");

        DatagramSocket clientSocket = new DatagramSocket(); 
        InetAddress address = InetAddress.getByName(args[0]);
        byte[] buf = new byte[256];
        int port = Integer.parseInt(args[1]);

        String reqString = "";

        for(int i = 2; i < args.length; i++) {
            reqString = reqString + args[i] + " ";
        }

        reqString.trim();
        System.out.println("Request: " + reqString);
        buf = reqString.getBytes();
        
        DatagramPacket requestDatagram = new DatagramPacket(buf,buf.length, address, port); 
        clientSocket.send(requestDatagram);
        System.out.println("Request sent");

        byte[] rbuf = new byte[256];
        DatagramPacket responsePacket = new DatagramPacket(rbuf, rbuf.length);
        clientSocket.receive(responsePacket);
        System.out.println("Response received");
        String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
        System.out.println("Client: " + reqString + ": " + response);

        clientSocket.close();
    }
}
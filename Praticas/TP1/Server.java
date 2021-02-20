import java.io.*;
import java.net.*;
import java.util.HashMap;

public class Server{
    public static void main(String [] args) throws IOException{

        if(args.length != 1) {
            System.out.println("Usage: Server <port number>");
            return;
        }

        int port = Integer.parseInt(args[0]);

        HashMap<String,String> table = new HashMap<>();

        DatagramSocket serverSocket = new DatagramSocket(null);
        InetSocketAddress innetSocket = new InetSocketAddress(InetAddress.getByName("localhost"), port);
        serverSocket.bind(innetSocket);
        System.out.println("Server is up and running in port " + port);

        while(true) {
            try {
                byte[] rbuf = new byte[256];
                DatagramPacket packet = new DatagramPacket(rbuf, rbuf.length);        
                serverSocket.receive(packet);
                System.out.println("Request received");
                String[] response = new String(packet.getData(), 0, packet.getLength()).split(" ");
                
                byte[] sbuf = new byte[256];

                if(response[0].equals("REGISTER")) {
                    if(table.get(response[1]) != null) {
                        System.out.println("Server: " + response[0] + " " + response[1] + " " + response[2]);
                        sbuf = String.valueOf(-1).getBytes();
                    }
                    else {
                        System.out.println("Server: " + response[0] + " " + response[1] + " " + response[2]);
                        table.put(response[1], response[2]);

                        sbuf = String.valueOf(table.size()).getBytes();
                    }
                }

                else if(response[0].equals("LOOKUP")) {
                    System.out.println("bla");
                    if(table.get(response[1]) != null) {
                        System.out.println("Server: " + response[0] + " " + response[1]);
                        sbuf = String.valueOf(table.get(response[1])).getBytes();
                    }
                    else {
                        System.out.println("Server: " + response[0] + " " + response[1]);
                        String ans = "NOT_FOUND";
                        sbuf = ans.getBytes();
                    }
                }

                InetAddress clientAddress = packet.getAddress();
                int clientPort = packet.getPort();

                DatagramPacket responsePacket = new DatagramPacket(sbuf, sbuf.length, clientAddress, clientPort);
                serverSocket.send(responsePacket);
                System.out.println("Response sent");
            }
            catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }

        serverSocket.close();
    }
}
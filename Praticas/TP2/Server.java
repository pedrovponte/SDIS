import java.io.*;
import java.net.*;
import java.util.HashMap;

public class Server{

    public static void main(String [] args) throws IOException{

        if(args.length != 3) {
            System.out.println("Usage: Server <srvc_port> <mcast_addr> <mcast_port>");
            return;
        }

        int servicePort = Integer.parseInt(args[0]);
        InetAddress multicastAddress = InetAddress.getByName(args[1]);
        int multicastPort = Integer.parseInt(args[2]);
        String localhost = InetAddress.getLocalHost().getHostAddress();

        MulticastThread advertiseTask = new MulticastThread(servicePort, localhost, multicastAddress, multicastPort);
        advertiseTask.start();
        
        DatagramSocket serverSocket = new DatagramSocket(servicePort);

        HashMap<String,String> table = new HashMap<>();

        System.out.println("Server is up and running in port " + servicePort);

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

    /*private class AdvertiserService extends TimerTask {
        private int servicePort;
        private InetAddress multicastAddress;
        private int multicastPort;
        private DatagramSocket serverSocket;
        private MulticastSocket multicastSocket;
        
        public AdvertiserService(int servicePort, InetAddress multicastAddress, int multicastPort, DatagramSocket serverSocket, MulticastSocket multicastSocket) {
            this.servicePort = servicePort;
            this.multicastAddress = multicastAddress;
            this.multicastPort = multicastPort;
            this.serverSocket = serverSocket;
            this.multicastSocket = multicastSocket;
        }

		@Override
		public void run() {
            try {
                byte[] out = new String(InetAddress.getLocalHost().getHostAddress() + " " + servicePort).getBytes();
                DatagramPacket advertise = new DatagramPacket(out, out.length, multicastAddress, multicastPort);
                multicastSocket.send(advertise);
                System.out.println("multicast: " + multicastAddress + " " + multicastPort + ": " + serverSocket.getLocalAddress() + " " + serverSocket.getLocalPort());
            }
            catch (IOException e) {
                e.printStackTrace();
            }
		}
    }*/
}
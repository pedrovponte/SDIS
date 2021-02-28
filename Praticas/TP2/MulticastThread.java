import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Timer;
import java.util.TimerTask;

public class MulticastThread extends Thread {
    private int servicePort;
    private InetAddress multicastAddress;
    private int multicastPort;
    private String serviceAddress;
    private DatagramSocket socket;
    private DatagramPacket advertise;
    
    public MulticastThread(int servicePort, String serviceAddress, InetAddress multicastAddress, int multicastPort) throws SocketException {
        this.servicePort = servicePort;
        this.multicastAddress = multicastAddress;
        this.multicastPort = multicastPort;
        this.serviceAddress = serviceAddress;
        this.socket = new DatagramSocket(3000);

        byte[] out = new String(serviceAddress + " " + servicePort).getBytes();
        advertise = new DatagramPacket(out, out.length, multicastAddress, multicastPort);
    }

    public void run() {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    System.out.println("bla bla");
                    socket.send(advertise);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("multicast: " + multicastAddress + " " + multicastPort + ": " + serviceAddress + " " + servicePort);
            }
        };

        Timer timer = new Timer();
        timer.schedule(timerTask, 30000, 1000);
    }
}

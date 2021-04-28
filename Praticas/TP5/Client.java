import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;

public class Client {
    public static void main(String[] args) {
        if (args.length < 4) {
            System.out.println("Usage: java Client <host> <port> <oper> <opnd>* <cypher-suite>* ");
            return;
        }

        //set the type of trust store
        System.setProperty("javax.net.ssl.trustStoreType","JKS");

        //set the password with which the truststore is encripted
        System.setProperty("javax.net.ssl.trustStorePassword", "123456");

        //set the name of the trust store containing the server's public key and certificate           
        System.setProperty("javax.net.ssl.trustStore", "truststore");

        //set the password with which the client keystore is encripted
        System.setProperty("javax.net.ssl.keyStorePassword","123456");

        //set the name of the keystore containing the client's private and public keys
        System.setProperty("javax.net.ssl.keyStore","clientKeyStore");

        String command = buildCommand(args);

        String host = args[0];
        int port = Integer.parseInt(args[1]);

        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();

        SSLSocket clientSocket = null;

        try {
            clientSocket = (SSLSocket) factory.createSocket(host, port);
        }
        catch (IOException e) {
            System.out.println("Client - Failed to create SSLSocketFactory");  
            e.getMessage();  
            return; 
        }

        try {
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        
            out.println(command);

            String received = in.readLine();

            System.out.println("SSLClient: " + command + " : " + received);

            out.close();
            in.close();
            clientSocket.close();
        }
        catch (IOException e) {
            e.getMessage();  
            return; 
        }
    }

    private static String buildCommand(String[] args) {
        String[] ops = new String[args.length - 2];
        System.arraycopy(args, 2, ops, 0, args.length - 2);

        if (ops.length == 2)
            return String.join(" ", ops[0], ops[1]);
        else if (ops.length == 3)
            return String.join(" ", ops[0], ops[1], ops[2]);

        return "";
    }
}
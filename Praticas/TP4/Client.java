import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) {
        if(args.length < 4 || args.length > 5 || (args[2] == "REGISTER" && args.length != 5) || (args[2] == "LOOKUP" && args.length != 4) /*|| (args[2] != "REGISTER" && args[2] != "LOOKUP")*/) {
            System.out.println("Usage:");
            System.out.println("\t Client <host_name> <port_number> REGISTER <DNS name> <IP address>");
            System.out.println("\t Client <host_name> <port_number> LOOKUP <DNS name>");
            return;
        }

        String address = args[0];
        int port = Integer.parseInt(args[1]);

        try(Socket socket = new Socket(address, port)) {
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);

            String reqString = "";

            for(int i = 2; i < args.length; i++) {
                reqString = reqString + args[i] + " ";
            }

            reqString.trim();
            System.out.println("Request: " + reqString);
            writer.println(reqString);
            output.flush();

            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            String ans = reader.readLine();

            System.out.println(reqString + " :: " + ans);


        } catch (UnknownHostException e) {
            System.out.println("Server not found: " + e.getMessage());
 
        } catch (IOException e) {
            System.out.println("I/O error: " + e.getMessage());
        }
    }
}
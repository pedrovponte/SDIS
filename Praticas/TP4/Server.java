import java.io.*;
import java.net.*;
import java.util.HashMap;

public class Server {
    public static void main(String[] args) {
        Server server = new Server();

        if(args.length != 1) {
            System.out.println("Usage: Server <srvc_port>");
            return;
        }

        int srvc_port = Integer.parseInt(args[0]);
        HashMap<String,String> table = new HashMap<>();

        try(ServerSocket serverSocket = new ServerSocket(srvc_port)) {
            System.out.println("Server is listening on port " + srvc_port);

            while(true) {
                Socket socket = serverSocket.accept();

                InputStream input = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));
    
                String task = reader.readLine();
                System.out.println(task);

                String[] splitted = task.split(" ");
                System.out.println(splitted);
                
                String ans = server.runTask(splitted, table);

                OutputStream output = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(output, true);

                writer.println(ans);
                output.flush();

                socket.shutdownOutput();
                socket.close();
            }

        } catch (UnknownHostException e) { 
            System.out.println("Server not found: " + e.getMessage());
 
        } catch (IOException e) {
            System.out.println("I/O error: " + e.getMessage());
        }

    }

    public String runTask(String[] task, HashMap<String, String> table) {
        String ans = "";
        System.out.println(task[0]);
        System.out.println(task[1]);
        if(task[0].equals("REGISTER")) {
            if(table.get(task[1]) != null) {
                ans = String.valueOf(-1);
            }
            else {
                table.put(task[1], task[2]);
                ans = String.valueOf(table.size());
            }
            System.out.println(task[0] + " " + task[1] + task[2] + " :: " + ans);
        }

        else if(task[0].equals("LOOKUP")) {
            if(table.get(task[1]) != null) {
                ans = String.valueOf(table.get(task[1]));
            }
            else {
                ans = "NOT_FOUND";
            }
            System.out.println(task[0] + " " + task[1] + " :: " + ans);
        }
        return ans;
    }
}
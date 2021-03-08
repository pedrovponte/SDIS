import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {
    private String host_name;
    private String remote_object_name;
    private String oper;
    private String dns_name;
    private String ip_address;

    public static void main(String args[]) {
        Client client = new Client();
        
        if(client.parseArguments(args) != 0) {
            return;
        }

        try {
            Registry registry = LocateRegistry.getRegistry(client.host_name);
            RemoteInterface stub = (RemoteInterface) registry.lookup(client.remote_object_name);
            
            switch(client.oper) {
                case "REGISTER":
                    int responseRegister = stub.register(client.dns_name, client.ip_address);
                    System.out.println(client.oper + " " + client.dns_name + " " + client.ip_address + " :: " + responseRegister);
                    break;
                
                case "LOOKUP":
                    String responseLookup = stub.lookup(client.dns_name);
                    System.out.println(client.oper + " " + client.dns_name + " :: " + responseLookup);
                    break;
            }
        } catch(Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }

    public int parseArguments(String args[]) {
        if (args.length < 4 || args.length > 5 || (args[2] == "REGISTER" && args.length != 5)
                || (args[2] == "LOOKUP" && args.length != 4) /* || (args[2] != "REGISTER" && args[2] != "LOOKUP") */) {
            System.out.println("Usage:");
            System.out.println("\t Client <host_name> <remote_object_name> REGISTER <DNS name> <IP address>");
            System.out.println("\t Client <host_name> <remote_object_name> LOOKUP <DNS name>");
            return -1;
        }

        this.host_name = args[0];
        this.remote_object_name = args[1];
        this.oper = args[2];

        if (oper == "REGISTER") {
            this.dns_name = args[3];
            this.ip_address = args[4];
            return 0;
        } else {
            this.dns_name = args[3];
            return 0;
        }
    }
}

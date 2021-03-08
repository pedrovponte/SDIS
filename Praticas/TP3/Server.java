import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Hashtable;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Server implements RemoteInterface {
    private Hashtable<String, String> table;
    public static void main(String args[]) throws RemoteException{
        Server server = new Server();

        if(args.length != 1) {
            System.out.println("Usage:");
            System.out.println("\t Server <remote_object_name>");
            return;
        }

        server.createInterface(args);
    }

    public void createInterface(String args[]) throws RemoteException {
        RemoteInterface stub = (RemoteInterface) UnicastRemoteObject.exportObject(this, 0);

        // Bind the remote object's stub in the registry
        Registry registry = LocateRegistry.getRegistry();
        registry.rebind(args[0], stub);

        this.table = new Hashtable<String, String>();
    }

    @Override
    public int register(String dnsName, String ipAddress) throws RemoteException {
        if(this.table.get(dnsName) == null) {
            System.out.println("REGISTER " + dnsName + " :: " + ipAddress);
            this.table.put(dnsName, ipAddress);
            return this.table.size();
        }
        
        System.out.println("REGISTER" + dnsName + " :: " + ipAddress);
        return -1;
    }

    @Override
    public String lookup(String dnsName) throws RemoteException {
        if(this.table.get(dnsName) != null) {
            String ipAddress = this.table.get(dnsName);
            System.out.println("LOOKUP " + dnsName + " :: " + ipAddress);
            return ipAddress;
        }

        return null;
    }
}
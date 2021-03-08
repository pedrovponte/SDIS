import java.rmi.RemoteException;
import java.rmi.Remote;

public interface RemoteInterface extends Remote {
    int register(String dnsName, String ipAddress) throws RemoteException;
    String lookup(String dnsName) throws RemoteException; 
}

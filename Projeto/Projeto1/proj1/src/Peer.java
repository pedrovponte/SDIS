import java.net.*;
import java.nio.channels.Channel;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.FileSystems;

public class Peer implements RemoteInterface {
    private ChannelController MC;
    private ChannelController MDB;
    private ChannelController MDR;
    private TCPChannel tcpChannel;
    private int TCPport;
    private String protocolVersion;
    private static int peerId;
    private ScheduledThreadPoolExecutor threadExec;
    private static FileStorage storage;
    private ConcurrentHashMap<String, Integer> receivedChunkMessages;
    private ServerSocket serverSocket;

    public Peer(String protocolVersion, int id) {
        this.receivedChunkMessages = new ConcurrentHashMap<String, Integer>();
        this.protocolVersion = protocolVersion;
        peerId = id;
        System.out.println("ID: " + peerId);
        this.threadExec = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(300);
        System.out.println("--- Created Threads ---");

        if(new File("peer_" + peerId + "/storage.ser").exists()) {
            deserialization();
        }
        else {
            storage = new FileStorage();
        }

        this.TCPport = 6000 + peerId;

        try {
            this.serverSocket = new ServerSocket(this.TCPport);
        } catch(Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }


        /*ConcurrentHashMap<String, Integer> stored = this.getStorage().getStoredMessagesReceived();
        System.out.println("-------STORED-------");
        for(String key : stored.keySet()) {
            System.out.println("Key: " + key);
            System.out.println("Value: " + stored.get(key));
        }
        System.out.println("Total: " + stored.size());

        ArrayList<FileManager> files = this.getStorage().getFilesStored();
        System.out.println("-------FILES-------");
        for(int i = 0; i < files.size(); i++) {
            System.out.println("Key: " + i);
            System.out.println("Value: " + files.get(i).getPath());
        }
        System.out.println("Total: " + files.size());

        ConcurrentHashMap<String, Chunk> chunks = this.getStorage().getChunksStored();
        System.out.println("-------CHUNKS-------");
        for(String key : chunks.keySet()) {
            System.out.println("Key: " + key);
            System.out.println("Value: " + chunks.get(key));
        }
        System.out.println("Total: " + chunks.size());*/
    }

    public static void main(String[] args) {

        if (args.length != 9) {
            System.out.println(
                    "Usage: Peer <protocol_version> <peer_id> <service_access_point> <MC_IP_address> <MC_port> <MDB_IP_address> <MDB_port> <MDR_IP_address> <MDR_port>");
            return;
        }

        String protocolVersion = args[0];

        if(!((protocolVersion.equals("1.0")) || (protocolVersion.equals("2.0")))) {
            System.out.println("Invalid protocol version: " + protocolVersion + ". Available versions: '1.0' or '2.0'.");
            return;
        }

        int peerId = Integer.parseInt(args[1]);
        String serviceAccessPoint = args[2];
        String mcAddress = args[3];
        int mcPort = Integer.parseInt(args[4]);
        String mdbAddress = args[5];
        int mdbPort = Integer.parseInt(args[6]);
        String mdrAddress = args[7];
        int mdrPort = Integer.parseInt(args[8]);

        System.out.println("Protocol version: " + protocolVersion);
        System.out.println("Peer Id: " + peerId);
        System.out.println("Service Access Point: " + serviceAccessPoint);
        System.out.println("Mc address: " + mcAddress);
        System.out.println("Mc port: " + mcPort);
        System.out.println("MDB address: " + mdbAddress);
        System.out.println("MDB port: " + mdbPort);
        System.out.println("MDR address: " + mdrAddress);
        System.out.println("MDR port: " + mdrPort);

        Peer peer = new Peer(protocolVersion, peerId);

        try {
            RemoteInterface stub = (RemoteInterface) UnicastRemoteObject.exportObject(peer, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(serviceAccessPoint, stub);
            System.out.println("--- Running RMI Registry ---");
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace();
        }

        peer.createChannels(mcAddress, mcPort, mdbAddress, mdbPort, mdrAddress, mdrPort);
        System.out.println("--- Channels Created ---");

        peer.execChannels();
        System.out.println("--- Running Channels --- \n\n");

        // sends WORKING message in order to make all the other peers know that this peer is available and get the files deleted information in order to delete them and update chunksDistribution info
        if(protocolVersion.equals("2.0")) {
            // <Version> WORKING <PeerId> <CRLF><CRLF>
            String toSend = protocolVersion + " WORKING " + peerId + " \r\n\r\n";
            peer.getThreadExec().execute(new ThreadSendMessages(peer.getMC(), toSend.getBytes()));
            System.out.println("SENT: " + toSend);
        }

        // serialize data before close peer
        Runtime.getRuntime().addShutdownHook(new Thread(Peer::serialization));
    }


    public ChannelController getMC() {
        return this.MC;
    }


    public ChannelController getMDB() {
        return this.MDB;
    }

    
    public ChannelController getMDR() {
        return this.MDR;
    }

    
    public TCPChannel getTcpChannel() {
        return this.tcpChannel;
    }

    
    public String getProtocolVersion() {
        return this.protocolVersion;
    }

    
    public static int getPeerId() {
        return peerId;
    }

    
    public ScheduledThreadPoolExecutor getThreadExec() {
        return this.threadExec;
    }

    
    public FileStorage getStorage() {
        return storage;
    }

    
    public ConcurrentHashMap<String,Integer> getReceivedChunkMessages() {
        return this.receivedChunkMessages;
    }


    // increment the number of CHUNK messages received for a given chunkId, in order to know if some peer already has sent the chunk chunkId to the initiator peer
    public void incrementReceivedChunkMessagesNumber(String chunkId) {
        Integer number = this.receivedChunkMessages.get(chunkId);
        if(number == null) {
            this.receivedChunkMessages.put(chunkId, 1);
        }
        else {
            this.receivedChunkMessages.replace(chunkId, number + 1);
        }
    }


    // creates multicast channels
    public void createChannels(String mcAddress, int mcPort, String mdbAddress, int mdbPort, String mdrAddress,
            int mdrPort) {
        this.MC = new ChannelController(mcAddress, mcPort, this);
        this.MDB = new ChannelController(mdbAddress, mdbPort, this);
        this.MDR = new ChannelController(mdrAddress, mdrPort, this);
    }


    // 3 different threads executing multicast channels, 1 for each other
    public void execChannels() {
        this.threadExec.execute(this.MC);
        this.threadExec.execute(this.MDB);
        this.threadExec.execute(this.MDR);
    }


    @Override
    public void backup(String path, int replication) {
        File backupFile = new File(path);

        if(!backupFile.exists()) {
            System.out.println("The file - " + path + " - doesn't exist.");
            return;
        }

        FileManager fileManager = new FileManager(path, replication, peerId);

        String fileIDNew = fileManager.getFileID();

        for(int i = 0; i < storage.getFilesStored().size(); i++) {
            if(storage.getFilesStored().get(i).getFileID().equals(fileIDNew)) {
                System.out.println("File already backed up by this peer.");
                return;
            }
        }

        storage.addFile(fileManager);

        if(storage.hasDeletedFile(fileManager.getFileID())) {
            storage.removeDeletedFile(fileManager.getFileID());
        }

        ArrayList<Chunk> fileChunks = fileManager.getFileChunks();

        for(int i = 0; i < fileChunks.size(); i++) {
            // <Version> PUTCHUNK <SenderId> <FileId> <ChunkNo> <ReplicationDeg> <CRLF><CRLF><Body>
            String header = this.protocolVersion + " PUTCHUNK " + peerId + " " + fileManager.getFileID() + " " + fileChunks.get(i).getChunkNo() + " " + fileChunks.get(i).getReplication() + " \r\n\r\n";
            
            try {
                byte[] headerBytes = header.getBytes(StandardCharsets.US_ASCII);
                byte[] body = fileChunks.get(i).getChunkMessage();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
                outputStream.write(headerBytes);
                outputStream.write(body);
                byte[] message = outputStream.toByteArray();
                
                if(!(storage.hasRegisterStore(fileManager.getFileID(), fileChunks.get(i).getChunkNo()))) {
                    storage.createRegisterToStore(fileManager.getFileID(), fileChunks.get(i).getChunkNo());
                }

                // send threads
                this.threadExec.execute(new ThreadSendMessages(this.MDB, message));
                this.threadExec.schedule(new ThreadCountStored(this, replication, fileManager.getFileID(), i, this.MDB, message), 1, TimeUnit.SECONDS);

                System.out.println("SENT: "+ header);
            } catch(UnsupportedEncodingException e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            } catch(IOException e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }


    @Override
    public void restore(String path) {
        //<Version> GETCHUNK <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
        File backupFile = new File(path);

        if(!backupFile.exists()) {
            System.out.println("The file - " + path + " - doesn't exist.");
            return;
        }

        if (this.protocolVersion.equals("2.0")){
            this.tcpChannel = new TCPChannel(this.serverSocket, this);
            this.threadExec.execute(tcpChannel);
        }

        ArrayList<FileManager> files = this.getStorage().getFilesStored();
        ArrayList<String> filesNames = new ArrayList<String>();

        for(int i = 0; i < files.size(); i++) {
            filesNames.add(files.get(i).getPath());
        }

        if(!filesNames.contains(path)){
            System.out.println("File " + path + " never backed up in this peer");
            return;
        }

        for(int i = 0; i < files.size(); i++) {
            if(files.get(i).getPath().equals(path)) {
                storage.addFileToRestore(files.get(i).getFileID());
                for(int j = 0; j < files.get(i).getFileChunks().size(); j++) {
                    String message = "";
                    if (this.protocolVersion == "1.0") {
                        message = this.protocolVersion + " GETCHUNK " + peerId + " " + files.get(i).getFileID() + " " + j + " \r\n\r\n";
                    }
                    else {
                        message = this.protocolVersion + " GETCHUNK " + peerId + " " + files.get(i).getFileID() + " " + j + " " + this.TCPport + " \r\n\r\n";
                    }
                    try {
                        this.threadExec.execute(new ThreadSendMessages(this.MC, message.getBytes(StandardCharsets.US_ASCII)));

                        System.out.println("SENT: " + message);
                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                        e.printStackTrace();
                    }
                }
                this.threadExec.schedule(new ManageRestoreThread(this, files.get(i)), 10, TimeUnit.SECONDS);
            }
        }
    }


    @Override
    public void delete(String path) {
        // <Version> DELETE <SenderId> <FileId> <CRLF><CRLF>
        File backupFile = new File(path);

        if(!backupFile.exists()) {
            System.out.println("The file - " + path + " - doesn't exist.");
            return;
        }
        
        ArrayList<FileManager> files = this.getStorage().getFilesStored();
        ArrayList<String> filesNames = new ArrayList<String>();

        for(int i = 0; i < files.size(); i++) {
            filesNames.add(files.get(i).getPath());
        }

        if(!filesNames.contains(path)){
            System.out.println("File " + path + " never backed up in this peer");
            return;
        }

        for(int i = 0; i < files.size(); i++) {
            if(files.get(i).getPath().equals(path)) {
                // This message does not elicit any response message. An implementation may send this message as many times as it is deemed necessary
                for(int j = 0; j < 5; j++) {
                    String message = this.protocolVersion + " DELETE " + peerId + " " + files.get(i).getFileID() + " \r\n\r\n";
                    try {
                        this.threadExec.execute(new ThreadSendMessages(this.MC, message.getBytes(StandardCharsets.US_ASCII)));

                        System.out.println("SENT: " + message);
                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                        e.printStackTrace();
                    }
                }

                ConcurrentHashMap<String,Integer> storedMessages = this.getStorage().getStoredMessagesReceived();
                for(String key : storedMessages.keySet()) {
                    for(int k = 0; k < files.get(i).getFileChunks().size(); k++) {
                        String chunkId = files.get(i).getFileID() + "_" + k;
                        if(key.equals(chunkId)) {
                            this.getStorage().deleteStoreMessage(chunkId);
                        }
                    }
                }

                if(this.protocolVersion.equals("1.0")) {
                    storage.deleteChunksDistribution(files.get(i).getFileID());
                }

                this.getStorage().deleteFile(files.get(i));
                break;
            }
        }
    }


    @Override
    public void reclaim(int maximum_disk_space) {
        // <Version> REMOVED <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
        int max_space = maximum_disk_space * 1000;

        storage.setCapacity(max_space);

        int occupiedSpace = storage.getPeerOccupiedSpace();

        int spaceToFree = occupiedSpace - max_space;

        if(spaceToFree > 0) {
            ConcurrentHashMap<String, Chunk> chunksStored = this.getStorage().getChunksStored();
            ArrayList<Chunk> chunks = new ArrayList<>();

            for(String key : chunksStored.keySet()) {
                chunks.add(chunksStored.get(key));
            }

            // descendant ordered list to start delete biggest chunks first
            Collections.sort(chunks, Comparator.comparing(Chunk::getSize));
            Collections.reverse(chunks);

            for(int i = 0; i < chunks.size(); i++) {
                String chunkId = chunks.get(i).getFileId() + "_" + chunks.get(i).getChunkNo();
                storage.deleteChunk(chunkId);

                String message = this.protocolVersion + " REMOVED " + peerId + " " + chunks.get(i).getFileId() + " " + chunks.get(i).getChunkNo() + " \r\n\r\n";
                try {
                    this.threadExec.execute(new ThreadSendMessages(this.MC, message.getBytes(StandardCharsets.US_ASCII)));

                    System.out.println("SENT: " + message);
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                    e.printStackTrace();
                }

                spaceToFree -= chunks.get(i).getSize();

                String name = "peer_" + peerId + "/backup/" + chunkId;

                File filename = new File(name);

                filename.delete();

                if(spaceToFree <= 0) {
                    return;
                }
            }
        }
    }


    @Override
    public void state() {
        System.out.println();
        System.out.println("---------FILES BACKED UP---------");
        if(storage.getFilesStored() != null && !storage.getFilesStored().isEmpty()) {
            for(int i = 0; i < storage.getFilesStored().size(); i++) {
                String path = storage.getFilesStored().get(i).getPath();
                String fileId = storage.getFilesStored().get(i).getFileID();
                int desiredReplication = storage.getFilesStored().get(i).getReplication();
                System.out.println("PATHNAME: " + path);
                System.out.println("FILE ID: " + fileId);
                System.out.println("DESIRED REPLICATION: " + desiredReplication);
    
                System.out.println("------FILE CHUNKS------");
                for(int j = 0; j < storage.getFilesStored().get(i).getFileChunks().size(); j++) {
                    int chunkNo = storage.getFilesStored().get(i).getFileChunks().get(j).getChunkNo();
                    String chunkId = fileId + "_" + chunkNo;
                    System.out.println("CHUNK NO: " + chunkNo);
                    int perceivedReplication = storage.getPerceivedReplication(chunkId);
                    System.out.println("PERCEIVED REPLICATION DEGREE: " + perceivedReplication);
                    System.out.println();
                }
                System.out.println("---------------------------");
            }
        }
        
        System.out.println();
        System.out.println("---------CHUNKS STORED---------");
        for(String key : storage.getChunksStored().keySet()) {
            int chunkNo = storage.getChunksStored().get(key).getChunkNo();
            int size = storage.getChunksStored().get(key).getSize();
            int desiredReplication = storage.getChunksStored().get(key).getReplication();
            String fileId = storage.getChunksStored().get(key).getFileId();
            String chunkId = fileId + "_" + chunkNo;
            int perceivedReplication = storage.getPerceivedReplication(chunkId);
            System.out.println("CHUNK NO: " + chunkNo);
            System.out.println("SIZE: " + size / 1000 + " KBytes");
            System.out.println("DESIRED REPLICATION: " + desiredReplication);
            System.out.println("PERCEIVED REPLICATION: " + perceivedReplication);
            System.out.println();
        }
        
        System.out.println();
        int totalCapacity = storage.getCapacity();
        int occupiedCapacity = storage.getPeerOccupiedSpace();
        System.out.println("STORAGE CAPACITY: " + totalCapacity / 1000 + " KBytes");
        System.out.println("USED CAPACITY: " + occupiedCapacity / 1000 + " KBytes");
    }


    // https://www.tutorialspoint.com/java/java_serialization.htm
    public void deserialization() {
        System.out.println("Deserializing data...");
        try {
            String fileName = "peer_" + peerId + "/storage.ser";
            

            FileInputStream fileIn = new FileInputStream(fileName);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            storage = (FileStorage) in.readObject();
            in.close();
            fileIn.close();
        } catch (IOException i) {
            System.err.println(i.getMessage());
            i.printStackTrace();
            return;
        } catch (ClassNotFoundException c) {
            System.out.println("Storage class not found");
            c.printStackTrace();
            return;
        }
    }


    // https://www.tutorialspoint.com/java/java_serialization.htm
    private static void serialization() {
        System.out.println("Serializing data...");
        try {
            String fileName = "peer_" + getPeerId() + "/storage.ser";
            File directory = new File("peer_" + getPeerId());

            if (!directory.exists()){
                // System.out.println("Not exists dir");
                directory.mkdir();
                (new File(fileName)).createNewFile();
            } 
            else if(directory.exists()) {
                if(!(new File(fileName).exists())) {
                    (new File(fileName)).createNewFile();
                }
            }

            FileOutputStream fileOut = new FileOutputStream(fileName);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(storage);
            out.close();
            fileOut.close();
        } catch (IOException i) {
            System.err.println(i.getMessage());
            i.printStackTrace();
        }
    }
}

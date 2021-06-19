import java.util.concurrent.ConcurrentHashMap;
import java.util.*;
import java.io.File;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;

public class ManageRestoreThread implements Runnable {
    private Peer peer;
    private FileManager fileManager;

    public ManageRestoreThread(Peer peer, FileManager fileManager) {
        this.peer = peer;
        this.fileManager = fileManager;
    }


    // thread that checks if all chunks of a file are restored and then join all of then in a file in order to restore that. Finally, creates the restored file in the restore sub directory
    @Override
    public void run() {
        int chunksNumber = this.fileManager.getFileChunks().size();

        ConcurrentHashMap<String,byte[]> allChunks = this.peer.getStorage().getChunksRestored();
        ConcurrentHashMap<String, byte[]> fileChunks = new ConcurrentHashMap<String, byte[]>();

        for(String key : allChunks.keySet()) {
            if((key.split("_")[0]).equals(this.fileManager.getFileID())) {
                fileChunks.put(key, allChunks.get(key));
            }
        }

        // checks if all file chunks are restored
        if(fileChunks.size() != chunksNumber) {
            System.out.println("ERROR: Not all file chunks have been restored");
            this.peer.getStorage().deleteFileRestored(this.fileManager.getFileID());
            this.peer.getStorage().deleteChunksRestored(this.fileManager.getFileID());
            return;
        }
        else {
            System.out.println("All chunks restored, going to create file");
        }

        // create the chunk file in the peer directory
        String dir = "peer_" + this.peer.getPeerId();
        String restoreDir = "peer_" + this.peer.getPeerId() + "/" + "restore";
        String file = "peer_" + this.peer.getPeerId() + "/" + "restore" + "/" + this.fileManager.getFile().getName();
        File directory = new File(dir);
        File restoreDirectory = new File(restoreDir);
        File f = new File(file);

        try{
            if (!directory.exists()){
                directory.mkdir();
                restoreDirectory.mkdir();
                f.createNewFile();
            } 
            else {
                if (directory.exists()) {
                    if(restoreDirectory.exists()) {
                        f.createNewFile();
                    }
                    else {
                        restoreDirectory.mkdir();
                        f.createNewFile();
                    }
                } 
            }


            FileOutputStream fos = new FileOutputStream(f);
            BufferedOutputStream bos = new BufferedOutputStream(fos);

            ArrayList<byte[]> chunks = new ArrayList<byte[]>();

            for(int i = 0; i < fileChunks.size(); i++) {
                for(String key : fileChunks.keySet()) {
                    if(Integer.parseInt(key.split("_")[1]) == i) {
                        chunks.add(fileChunks.get(key));
                    }
                }
            }

            for(int i = 0; i < chunks.size(); i++) {
                bos.write(chunks.get(i));
            }

            bos.close();
            System.out.println("Restore finished");
            System.out.println();

        } catch(Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
        
    }
}

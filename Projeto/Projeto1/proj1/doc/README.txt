T1G09   

---------------WITHOUT SCRIPTS---------------

---Compile files---
Inside src folder: javac *.java

---Start RMI---
rmiregistry &

---Run a peer---

java Peer <protocol_version> <peer_id> <peer_ap> <MC_IP_address> <MC_port> <MDB_IP_address> <MDB_port> <MDR_IP_address> <MDR_port>
(ex.) java Peer 1.0 1 Peer1 224.0.0.15 8001 224.0.0.16 8002 224.0.0.17 8003

Where:
    - <protocol_version> is "1.0" (without enhancements) or "2.0" (with enhancements);
    - <peer_id> is an integer representing peer unique identifier;
    - <peer_ap> is the access point for RMI object;
    - <MC_IP_address>, <MDB_IP_address> and <MDR_IP_address> are IP addresses for multicast channels (MC, MDB and MDR channels respectively). The address should be in the range 224.0.0.0 to 239.255.255.255;
    - <MC_port>, <MDB_port> and <MDR_port> are ports for multicast channels (MC, MDB and MDR channels respectively)

---Test BACKUP Protocol---

java Client <peer_ap> BACKUP <file_path_name> <replication_degree>

---Test RESTORE Protocol---

java Client <peer_ap> RESTORE <file_path_name>

---Test DELETE Protocol---

java Client <peer_ap> DELETE <file_path_name>

---Test RECLAIM Protocol---

java Client <peer_ap> RECLAIM <maximum_disk_space_KB>

---Test STATE Protocol---

java Client <peer_ap> STATE

While running, each peer will create a folder <peer_$peer_id> where it will save the backup and restore files.


---------------WITHOUT SCRIPTS---------------  

---Compile files---
Inside src folder: sh ../scripts/compile.sh

This should create a build folder inside src if everything ok.

---Create peers directory tree---
Inside build folder: sh ../../setup.sh <peer_id>

This should create a <peer_$peer_id> folder inside build.

---Start RMI---
remiregistry &

---Run a peer---
Inside build folder: bash ../../scripts/peer.sh <protocol_version> <peer_id> <peer_ap> <MC_IP_address> <MC_port> <MDB_IP_address> <MDB_port> <MDR_IP_address> <MDR_port>

---Test BACKUP Protocol---

Inside build folder: bash ../../scripts/test.sh <peer_ap> BACKUP <file_path_name> <replication_degree>

---Test RESTORE Protocol---

Inside build folder: bash ../../scripts/test.sh <peer_ap> RESTORE <file_path_name>

---Test DELETE Protocol---

Inside build folder: bash ../../scripts/test.sh <peer_ap> DELETE <file_path_name>

---Test RECLAIM Protocol---

Inside build folder: bash ../../scripts/test.sh <peer_ap> RECLAIM <maximum_disk_space_KB>

---Test STATE Protocol---

Inside build folder: bash ../../scripts/test.sh <peer_ap> STATE 

---Clean up peer directory---

Inside build folder: bash ../../scripts/cleanup.sh <peer_id>
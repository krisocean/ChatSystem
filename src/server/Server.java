
package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

public class Server {
  
  public static ServerState serverState = new ServerState();
  static int clients_port;
  static ArrayList<Integer> portList; 
  static ArrayList<String> addressList;  
  static ArrayList<String> serverList; 
  static ArrayList<Integer> clientPorts;  
  static String serverId;
  static ChatRoom mainHall = new ChatRoom();
  static String server_address;
  static int coordination_port;
 
  public static void main(String[] args) throws IOException{
    ServerSocket serverSocket = null;
    CmdLineArgs cmdArgs = new CmdLineArgs();
    CmdLineParser parser = new CmdLineParser(cmdArgs);
    
    try{
      parser.parseArgument(args);
      serverId = cmdArgs.getServerId();
      ReadConfig readConf = new ReadConfig();
      
      // read the file in the server_conf path
      readConf.fread(cmdArgs.getServer_conf());
      
      // get all server information from the server configuration file
      
      portList = readConf.getPortList();
      addressList = readConf.getServer_address();
      serverList = readConf.getServerList();
      clientPorts = readConf.getClientPorts();
      
      
      for(String serverid: readConf.getServerList()){
        if(serverId.equals(serverid)){
          
          int index = readConf.getServerList().indexOf(serverid);
          String server_address = readConf.getServer_address().get(index);          
          clients_port = readConf.getClientPorts().get(index);          
          int coordination_port = readConf.getPortList().get(index);
          
          // create the main Hall 
          mainHall.setRoomName("MainHall-"+serverId);
          mainHall.setOwner("");
          //ChatRoom mainHall = new ChatRoom("MainHall-"+serverId, "");
        }
      }
      
      serverState = new ServerState (serverId, server_address, clients_port, coordination_port, 
    	  serverList, portList ,addressList, clientPorts, mainHall);
      
      serverState.setRoomToServer("MainHall-"+serverId, serverId);
      serverState.setRoomToAllServer("MainHall-"+serverId, serverId); 
      serverState.setOwnerToRoom("", "MainHall-"+serverId);
           
      serverSocket = new ServerSocket(clients_port);
      
      while(true){
    	  
    	    // accept an incoming client connection
    	    
        Socket socket = serverSocket.accept();
        
        // create one thread per connection
        ClientConnection clientConnection = new ClientConnection(socket, serverState);        
        clientConnection.start();       
      }
      
    }catch(CmdLineException e){
       e.printStackTrace();
    }catch(IOException e){
    	    e.printStackTrace();
    }finally{
    	    if(serverSocket!=null){
    	    		serverSocket.close();
    	    }
    }
  }

}

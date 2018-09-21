package server;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ServerState {

	private Map<Socket, String> userToServer = new HashMap<Socket, String>();
	Map<String, String> userToRoom = new HashMap<String, String>();
	Map<String, String> roomToServer= new HashMap<String, String>();
	Map<String, String> roomToAllServer= new HashMap<String, String>();
	Map<String, String> ownerToRoom= new HashMap<String, String>();
	private ArrayList<String> users = new ArrayList<String>();
	private ArrayList<ClientConnection> connectedClients;
	private String serverId;
	private String server_address;
	private int clients_port;
	private int coordination_port;
	private ArrayList<Integer> portList;
	private ArrayList<Integer> clientPorts;
	private ArrayList<String> addressList;
	private ArrayList<String> serverList;
	private ChatRoom mainHall;
	
	public ServerState(){
		
	}
	
	public ServerState(String serverId, String server_address, int client_port, int coordination_port,
			ArrayList<String> serverList, ArrayList<Integer> portList, ArrayList<String> addressList, 
			ArrayList<Integer> clientPorts, ChatRoom mainHall){
		
		this.serverId = serverId;
		this.server_address = server_address;
		this.clients_port = clients_port;
		this.coordination_port = coordination_port;
		this.serverList = serverList;
		this.portList = portList;
		this.addressList = addressList;
		this.clientPorts = clientPorts;
		this.mainHall = mainHall;
		

		
	}
	
	public synchronized void clinetConnect(ClientConnection client){
		connectedClients.add(client);
	}
	
	public synchronized void clientDisconnect(ClientConnection client){
		connectedClients.remove(client);
	}
	
	public synchronized ArrayList<ClientConnection> getClientList(){
		return connectedClients;
	}

	public synchronized String getServerId(){
		return serverId;
	}
	
	public synchronized String getServerAddress(){
		return server_address;
	}
	
	public synchronized int getCoordination_port(){
		return coordination_port;
	}
	
	public synchronized ArrayList<Integer> getPortList(){
		return portList;
	}
	
	public synchronized ArrayList<Integer> getClietPorts(){
		return clientPorts;
	}
	
	public synchronized ArrayList<String> getAddressList(){
		return addressList;
	}
	
	public synchronized ArrayList<String> getServerList(){
		return serverList;
	}
	
	public synchronized ArrayList<String> getUser(){
		return users;
	}
	
	public synchronized ChatRoom getMainHall(){
		return mainHall;
	}
	
	public synchronized void addUser(String userName){
		this.users.add(userName);
	}
	
	public synchronized void setUserToServer(Socket socket, String string){
		this.userToServer.put(socket, string);
	}
	
	public synchronized void setRoomToServer(String roomid, String serverid){
		roomToServer.put(roomid, serverid);
	}
	
	public synchronized Map<String, String> getRoomToServer(){
		return roomToServer;
	}
	
	public synchronized void setUserToRoom(String user, String roomid){
		this.userToRoom.put(user, roomid);
	}
	
	public synchronized void removeUserToRoom(String user){
		this.userToRoom.remove(user);
	}
	
	public synchronized void removeUserToServer(Socket socket){
		this.userToServer.remove(socket);
	}
	
	
	public synchronized Map<String, String> getUserToRoom(){
		return userToRoom;
	}
	
	public synchronized void setRoomToAllServer(String roomid, String serverid){
		this.roomToAllServer.put(roomid, serverid);
	}
	
	public synchronized Map<String, String> getRoomToAllServer(){
		return roomToAllServer;
	}
	
	public synchronized void setOwnerToRoom(String owner, String roomid){
		this.ownerToRoom.put(owner, roomid);
	}
	
	
	public synchronized Map<String, String> getOwnerToRoom(){
		return ownerToRoom;
	}
	
	public synchronized Map<Socket, String> getUserToServer(){
		return userToServer;
	}

}

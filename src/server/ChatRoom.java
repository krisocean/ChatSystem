package server;

import java.util.ArrayList;
import java.util.Map;

public class ChatRoom {
	private String roomName;
	private String owner;
	private ArrayList<String> users = new ArrayList<String>();
	private Map<String, String > userToRoom;
	
	public ChatRoom(){
		
	}
	
	public ChatRoom(String roomName, String owner){
		this.roomName = roomName;
		this.owner = owner;
	}
	
	public String getRoomName(){
		return roomName;
	}
	
	public void setRoomName(String roomName){
		this.roomName = roomName;
	}
	
	public String getOwner(){
		return owner;
	}
	
	public void setOwner(String owner){
		this.owner = owner;
	}
	
	public void addUser(String user){
		users.add(user);
	}
	
	public ArrayList<String> getUser(String user){
		return users;
	}
	
	
	public void removeUser(String user){
		users.remove(user);
	}
	
	public ArrayList<String> getUser(){
		return users;
	}
	
	public void setMap(String identity, String roomName){
		this.userToRoom.put(identity, roomName);	
	}
	
}

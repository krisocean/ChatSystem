package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class ServerReceive extends Thread {
		private Socket receiveSocket;
		private ArrayList<String> locked;
		private ArrayList<String> lockedRoom;
		private int elemNumb;
		private Socket socket;
		private ServerState serverState;
		
	public ServerReceive(Socket receiveSocket, int elemNumb, Socket socket, 
			 ServerState serverState){
		    try{
		    	this.receiveSocket = receiveSocket;
		    	this.elemNumb = elemNumb;
		    	this.socket = socket;
		    	this.serverState = serverState;
		    	
		    }catch(Exception e){
		    	e.printStackTrace();
		    }
		}
	@SuppressWarnings("unchecked")
	public void run(){
		try{
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			DataInputStream serverIn = new DataInputStream(receiveSocket.getInputStream());
			DataOutputStream serverOut = new DataOutputStream(receiveSocket.getOutputStream());
			
			while(true){
				JSONParser parser = new JSONParser();
				String input = serverIn.readUTF();
				JSONObject msg = (JSONObject)parser.parse(input);
				String type = (String)msg.get("type");
				
				if(type.equals("lockidentity")){
					int elemNumber = msg.size();
					String identity = (String)msg.get("identity");
					if(elemNumber == 3){
						JSONObject lockidentity = new JSONObject();
						lockidentity.put("type", "lockidentity");
						lockidentity.put("serverid", serverState.getServerId());
						lockidentity.put("identity",identity);
						if(serverState.getUser().contains(identity)){
							lockidentity.put("locked", "false");
						}else{
							lockidentity.put("locked", "true");
						}
						// send a response back 
						serverOut.writeUTF(lockidentity.toJSONString());
						serverOut.flush();	
					
					} else {
						locked.add((String)msg.get("locked"));
						// if the coordinating server receives all the lock identity replies
						if((locked.size() == (elemNumb-1))&&(locked.contains("flase"))){
							JSONObject newidentity = new JSONObject();
							newidentity.put("type", "newidentity");
							newidentity.put("approved", "false");
							locked.clear();
							// new identity send to client
							out.writeUTF(newidentity.toJSONString());
							out.flush();
							
							JSONObject releaseidentity = new JSONObject();
 							releaseidentity.put("type" , "releaseidentity");
 							releaseidentity.put("serverid", serverState.getServerId());
 							releaseidentity.put("identity",identity);
 							
 							ServerSend serverSend = new ServerSend(serverState,releaseidentity);
							serverSend.start();
							
						}else if((locked.size() == (elemNumb-1))&&(!locked.contains("flase"))){
							JSONObject newidentity = new JSONObject();
							newidentity.put("type", "newidentity");
							newidentity.put("approved", "ture");
							locked.clear();
							out.writeUTF(newidentity.toJSONString());
							out.flush();
							
							JSONObject releaseidentity = new JSONObject();
 							releaseidentity.put("type" , "releaseidentity");
 							releaseidentity.put("serverid", serverState.getServerId());
 							releaseidentity.put("identity",identity);
 							
 							ServerSend serverSend = new ServerSend(serverState,releaseidentity);
							serverSend.start();
							
							//broadcast room change information to all members 
							JSONObject roomchange = new JSONObject();
							roomchange.put("type", "roomchange");
							roomchange.put("identity", identity);
							roomchange.put("former", "");
							roomchange.put("roomid", serverState.getMainHall().getRoomName());
							
							
									
						}
					}
								
				}else if(type.equals("releaseidentity")){
					String identity = (String)msg.get("identity");
					
					// release identity 
					if(((String)msg.get("serverid")).equals(serverState.getServerId())){
						
						//place new identity into the mainHall
						serverState.getMainHall().addUser(identity);
						serverState.addUser(identity);
						
						// every client identity correspond to one socket 
						serverState.setUserToServer(socket, identity);
						String roomid = serverState.getMainHall().getRoomName();
						serverState.setUserToRoom(identity, roomid);
						
					}
				}else if(type.equals("lockroomid")){
					int elemNumber = msg.size();
					String roomid = (String)msg.get("roomid");
					if(elemNumber == 3){
						JSONObject lockroomid = new JSONObject();
						lockroomid.put("type", lockroomid);
						lockroomid.put("serverid", serverState.getServerId());
						lockroomid.put("roomid", roomid);
						
						// receiving server reply with a lockroomid indicating
						// its vote 
						if(checkRoomid(roomid)){
							lockroomid.put("locked", "true");
						}else{
							lockroomid.put("locked", "false");
						}
						serverOut.writeUTF(lockroomid.toJSONString());
						serverOut.flush();					
					}else{
						
						// server receive indicating messages from other servers
						lockedRoom.add((String)msg.get("locked"));
						JSONObject releaseroomid = new JSONObject();
						releaseroomid.put("type", "releaseroomid");
						releaseroomid.put("serverid", serverState.getServerId());
						releaseroomid.put("roomid", roomid);
						
						// if at least one server deny the lock 
						if((lockedRoom.size() == (elemNumb-1))&&(locked.contains("false"))){
							releaseroomid.put("approved", "false");
							lockedRoom.clear();
							
							// server sends a releaseroomid request to other servers
							ServerSend serverSend = new ServerSend(serverState, releaseroomid);
							serverSend.start();
							
							
						}else if((lockedRoom.size() == (elemNumb-1))&&(!locked.contains("false"))){
							releaseroomid.put("approved", "true");
							lockedRoom.clear();
							ServerSend serverSend = new ServerSend(serverState, releaseroomid);
							serverSend.start();					
						}
					}		
				}else if(type.equals("releaseroomid")){
					
					// server release the lock
					String approved = (String)msg.get("approved");
					String serverid = (String)msg.get("serverid");
					String roomid = (String)msg.get("roomid");
					JSONObject createroom = new JSONObject();
					createroom.put("type", "createroom");
					createroom.put("roomid", roomid);
					if(approved.equals("true")){
						String identity = serverState.getUserToServer().get(socket);
						serverState.setRoomToServer(roomid, serverid);
						serverState.setRoomToAllServer(roomid, serverid);
						
						JSONObject globalroomid =new JSONObject();
						globalroomid.put("type", globalroomid);
						globalroomid.put("roomid", roomid);
						globalroomid.put("serverid", serverid);
						
						// send room info to other severs, store as global chat room 
						ServerSend serverSend = new ServerSend(serverState, globalroomid);		
						createroom.put("approved", "true");
						
						// now create a room in the server, store the client
						// as owner and a member of this room
						serverState.setOwnerToRoom(identity, roomid);
						serverState.setUserToRoom(identity, roomid);
						out.writeUTF(createroom.toJSONString());
						out.flush();
						
						JSONObject roomchange = new JSONObject();
						roomchange.put("type", "roomchange");
						roomchange.put("identity", identity);
						String former = serverState.userToRoom.get(identity);
						roomchange.put("former" , former);
						roomchange.put("roomid", roomid); 		
						
						//broadcast room change info to all clients in
						//previous room
						broadcast(former, roomchange);					
						
					}else{
						createroom.put("approved", "false");
						out.writeUTF(createroom.toJSONString());
						out.flush();
					}
				
				}else if(type.equals("globalroomid")){
					
					// receive global chat room info 
					String roomid = (String)msg.get("roomid");
					String serverid = (String)msg.get("serverid");
					serverState.setRoomToAllServer(roomid, serverid);				
					String identity = (String)msg.get("identity");
					serverState.setUserToRoom(identity, roomid);
					
				}
				
				
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public boolean checkRoomid(String roomid){
		boolean boo = true;
		for(Map.Entry<String, String> entry: serverState.getRoomToServer().entrySet()){
			if(entry.getValue().equals(roomid)){
				boo = false;
				break;
			}
		}		
		return boo;
	}
	
	public void broadcast(String roomid, JSONObject send) throws IOException{
		ArrayList<String> roomMembers = new ArrayList<String>();		
		
		// get all the room members of the room 
		for(Map.Entry<String, String> entry: serverState.getUserToRoom().entrySet()){
			if(entry.getValue().equals(roomid)){
				roomMembers.add(entry.getKey());
			}
		}
		
		// get all members' sockets 
		ArrayList<Socket> socketList = new ArrayList<Socket>();
		for(String roommember: roomMembers){
			for(Map.Entry<Socket, String> entry: serverState.getUserToServer().entrySet()){
				if(entry.getValue().equals(roommember)){
					socketList.add(entry.getKey());
				}
			}
		}
		// send the message among all sockets
		for(Socket socket: socketList){
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			out.writeUTF(send.toJSONString());
			out.flush();
		}
		
	}
}

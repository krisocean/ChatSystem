package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.util.ArrayList;
import java.util.Map;

public class ClientConnection extends Thread {
		
	
	private Socket socket;
    private ServerState serverState;	

		public ClientConnection(Socket socket, ServerState serverState){
			try{
				this.socket = socket;		
				this.serverState= serverState;
			
			}catch(Exception e){
				e.printStackTrace();
			}
			
		}
		
		@SuppressWarnings("unchecked")
		public void run(){
			
			try{
				DataInputStream in = new DataInputStream(socket.getInputStream());
				DataOutputStream out = new DataOutputStream(socket.getOutputStream());
				
				// listening incoming message from client socket 
				while(true){
					JSONParser parser = new JSONParser();
					String input = in.readUTF();
					
					// parse the message into a JSONObject
					JSONObject msg = (JSONObject)parser.parse(input);
					String type = (String)msg.get("type");
										
					// server start a  receive thread to listen the message from servers
					ServerSocket serverSocket = new ServerSocket(serverState.getCoordination_port());
						
					// receive socket is used to communicate with other servers
					Socket receiveSocket = serverSocket.accept();
					int elemNumb = serverState.getPortList().size();
					ServerReceive serverReceive = new ServerReceive(receiveSocket, elemNumb, socket, serverState);
					serverReceive.start();		
					
					if(type.equals("newidentity")){
 						String identity = (String)msg.get("identity");
 					
 						// check if this identity is valid and not in used by local clients
 						if(validName(identity)&& userDuplicateCheck(identity)){
 							
 							JSONObject lockidentity = new JSONObject();
 							lockidentity.put("type", "lockidentity");
 							lockidentity.put("serverid", serverState.getServerId());
 							lockidentity.put("identity", identity);
 							
 							// sever send lock identity to other servers
							ServerSend serverSend = new ServerSend(serverState, lockidentity);
 							serverSend.start();								
 						}	
								
					}else if(type.equals("list")){
						JSONObject list = new JSONObject();
						list.put("type", "roomlist");
						JSONArray chatRooms = new JSONArray();
						ArrayList<String> rooms = new ArrayList<String>(serverState.roomToAllServer.keySet());
						for(int i =0; i< rooms.size(); i++ ){
							String room = rooms.get(i);
							JSONObject temp = new JSONObject();
							temp.put("roomid", room);
							chatRooms.add(temp);
						}
						list.put("rooms", chatRooms);	
						
						// reply the client with the list info 
						out.writeUTF(list.toJSONString());
						out.flush();
					}else if(type.equals("who")){
						String identity = serverState.getUserToServer().get(socket);	
						JSONObject who = new JSONObject();
						who.put("type", "roomcontents");
						String roomid = serverState.getUserToRoom().get(identity);
						who.put("roomid", roomid);
						JSONArray users = new JSONArray();
						
						for(Map.Entry<String, String> entry: serverState.getUserToRoom().entrySet()){
							if(entry.getValue().equals(roomid)){
								users.add(entry.getKey());
							}
						}
						
						who.put("identities", users);
						
						// get the owner of the room 
						for(Map.Entry<String, String> entry: serverState.getOwnerToRoom().entrySet()){
							if(entry.getValue().equals(roomid)){
								String owner = entry.getKey();
								who.put("owner", owner);
							}
						}
                        // broadcast to all clients in the chat room
						broadcast(roomid, who);
												
					}else if(type.equals("join")){
						String identity = serverState.getUserToServer().get(socket);	
						String roomid = (String)msg.get("roomid");
						ArrayList<String> owners = new ArrayList<String>(serverState.getOwnerToRoom().keySet());
						ArrayList<String> globalRooms = new ArrayList<String>(serverState.getRoomToAllServer().keySet());
						ArrayList<String> localRooms = new ArrayList<String>(serverState.getRoomToServer().keySet());
						
						// check if this client is the owner of current chat room and 
						// if the join chat room is existed
						if((!owners.contains(identity))&& (globalRooms.contains(roomid))){
							JSONObject roomchange = new JSONObject();
							String former = serverState.getUserToRoom().get(identity);
							roomchange.put("type", "roomchange");
							roomchange.put("identity", identity);
							roomchange.put("former", former);
							roomchange.put("roomid", roomid);
							
							//if this chat room is in the same server
							if(localRooms.contains(roomid)){
						        
								//broadcast room change message to all members of the former
							    // chat room
							    broadcast(former, roomchange);
							    serverState.removeUserToRoom(identity);
							    serverState.setUserToRoom(identity, roomid);
								
								
							// if the chat room is managed by a different server 
							}else{
							    
								// server reply the client with route message redirecting it to
							    // another server
							  	JSONObject route = new JSONObject();
								route.put("type", "route");
								route.put("roomid", roomid);
								String serverid = serverState.getRoomToAllServer().get(roomid);
								int index = serverState.getServerList().indexOf(serverid);
								String host = serverState.getAddressList().get(index);
								String port = Integer.toString(serverState.getClietPorts().get(index));
								route.put("host", host);
								route.put("port", port);
								out.writeUTF(route.toJSONString());
								out.flush();
								
								// broadcast room change message to all members of the former
								// chat room
								broadcast(former, roomchange);
								
								// remove the client 
							    serverState.removeUserToRoom(identity);	
							    
							    JSONObject joinUser = new JSONObject();
							    joinUser.put("type", joinUser);
							    joinUser.put("identity", identity );
							    joinUser.put("roomid", roomid);
							    Socket newSocket = new Socket(serverState.getServerAddress(), serverState.getPortList().get(index));		    
							    	DataOutputStream newOut = new DataOutputStream(newSocket.getOutputStream());	
							    	newOut.writeUTF(joinUser.toJSONString());
							    	newOut.flush();
							    	
							    	// the client socket will not in this server 
							    	serverState.removeUserToServer(socket);
							}
								
						}else{
							
							// if join is not successful
							String formerRoom = serverState.getUserToRoom().get("identity");
							JSONObject roomchange = new JSONObject();
							roomchange.put("type", "roomchange");
							roomchange.put("identity", identity);
							roomchange.put("former", formerRoom );
							roomchange.put("roomid", roomid);
							
							// reply to fail room change info to client in current chat room
							out.writeUTF(roomchange.toJSONString());
							out.flush();
						}
										
					}else if(type.equals("message")){
						JSONObject message = new JSONObject();
						String content = (String)msg.get("content");
						message.put("type", "message");
						String identity = serverState.getUserToServer().get(socket);
						message.put("identity", identity);
						message.put("content", content);
						
						String chatroom = serverState.getUserToRoom().get(identity);	
						
						// broadcast message to current chat room
						broadcast(chatroom, message);
						
					}else if(type.equals("quit")){
						
						String identity = serverState.getUserToServer().get(socket);
						
						// remove the client from client list
						serverState.removeUserToServer(socket);
						
						// check if the client is the owner of a chat room
						if(serverState.getOwnerToRoom().keySet().contains(identity)){
							
							// delete protocol 
							String roomid = serverState.getOwnerToRoom().get(identity);
							JSONObject deleteroom = new JSONObject();
							deleteroom.put("type", "deleteroom");
							deleteroom.put("serverid", serverState.getServerId());
							deleteroom.put("roomid", roomid);
					        ServerSend serverSend = new ServerSend(serverState, deleteroom);
					        serverSend.start();
					                
					        // get all clients in the chat room
					        ArrayList<String> members = new ArrayList<String>();
					        for(Map.Entry<String, String> entry : serverState.getUserToRoom().entrySet()){
					        		if(entry.getValue().equals(roomid)){
					        			members.add(entry.getKey());
					        		}
					        }
					        
					        // board cast room change message to the delete room members
					        JSONObject roomchange = new JSONObject();
					        roomchange.put("type", "roomchange");
					        roomchange.put("former", roomid);
					        
					        // roomid value is empty string, indicate client is disconnecting
					        roomchange.put("roomid", "");
					        broadcast(roomid, roomchange);
					        for(String member: members){
					        	 	serverState.removeUserToRoom(member);
					        }
					        
					        // board cast room change message to the clients in the MainHall
					        broadcast(serverState.getMainHall().getRoomName(), roomchange);
					        
					        // finally, reply the client
					        JSONObject delete = new JSONObject();
					        delete.put("type", deleteroom);
					        delete.put("roomid", roomid);
					        delete.put("approved", "true");
					        out.writeUTF(delete.toJSONString());
					        out.flush();
					        
					        // server close the connection 
					        socket.close();
					        
						}
					}else if(type.equals("createroom")){
						String roomid = (String)msg.get("roomid");
						String identity = serverState.getUserToServer().get(socket);	
						
						// check if the roomId is valid and if the client is owner of 
					    // another room
						if(validName(roomid)&&checkOwner(identity)){
							JSONObject lockroomid = new JSONObject();
							lockroomid.put("type", "lockroomid");
							lockroomid.put("serverid", serverState.getServerId());
							lockroomid.put("roomid", roomid);
							
							// server send a lockroomid message to other servers
							ServerSend serverSend = new ServerSend(serverState, lockroomid);
							serverSend.start();
							
							//ServerSocket serverSocket = new ServerSocket(serverState.getCoordination_port());
							//Socket receiveSocket = serverSocket.accept();
							//int elemNumb = serverState.getPortList().size();
							//ServerReceive serverReceive = new ServerReceive(receiveSocket, elemNumb, socket, serverState);
							//serverReceive.start();
				
						}
					}else if(type.equals("movejoin")){
						String roomid = (String)msg.get("roomid");
						String former = (String)msg.get("former");
						
						// check if this chat room still exist 
						ArrayList<String> localRooms = new ArrayList<String>(serverState.getRoomToServer().keySet());
						String identity = serverState.getUserToServer().get(socket);
						JSONObject roomchange = new JSONObject();
						roomchange.put("type", "roomchange");
			            roomchange.put("identity", identity);
			            roomchange.put("former", former);						
						if(localRooms.contains(roomid)){
							
							//remove the client from the former room
							serverState.removeUserToRoom(identity);
							
							// place the client in the target chat room
							serverState.setUserToRoom(identity, roomid);
							
							roomchange.put("roomid", roomid);						
						}else{
							
							//remove the client from the former room
							serverState.removeUserToRoom(identity);
							
							// place the client in the MainHall
							serverState.setUserToRoom(identity, serverState.getMainHall().getRoomName());
							roomchange.put("roomid", serverState.getMainHall().getRoomName());
							
						}
						
						// broadcast room change info to all members in target chat room
						broadcast(roomid, roomchange);
						
						// reply the client 
						JSONObject serverchange = new JSONObject();
						serverchange.put("type", "serverchange");
						serverchange.put("approved", "true");
						serverchange.put("serverid", serverState.getServerId());
						out.writeUTF(serverchange.toJSONString());
						out.flush();  
						
					}else if(type.equals("deleteroom")){
						String roomid = (String)msg.get("roomid");
						String identity = serverState.getUserToServer().get(socket);
						
						// check if this client is the owner of the chat room
						// if the client is the owner of the chat room
						if(serverState.getOwnerToRoom().get(identity).equals(roomid)){
							
							// if successfully deleted, inform other servers
							JSONObject deleteroom = new JSONObject();
							deleteroom.put("type", "deleteroom");
							deleteroom.put("serverid", serverState.getServerId());
							deleteroom.put("roomid", roomid);
					        ServerSend serverSend = new ServerSend(serverState, deleteroom);
					        serverSend.start();
					                
					        // get all clients in the chat room
					        ArrayList<String> members = new ArrayList<String>();
					        for(Map.Entry<String, String> entry : serverState.getUserToRoom().entrySet()){
					        		if(entry.getValue().equals(roomid)){
					        			members.add(entry.getKey());
					        		}
					        }
					        
					        // board cast room change message to the delete room members
					        JSONObject roomchange = new JSONObject();
					        roomchange.put("type", "roomchange");
					        roomchange.put("former", roomid);
					        roomchange.put("roomid", serverState.getMainHall().getRoomName());
					        broadcast(roomid, roomchange);
					        for(String member: members){
					        	 	serverState.removeUserToRoom(member);
					        	 	serverState.setUserToRoom(member, serverState.getMainHall().getRoomName());
					        }
					        
					        // board cast room change message to the clients in the MainHall
					        broadcast(serverState.getMainHall().getRoomName(), roomchange);
					        
					        // finally, reply the client
					        JSONObject delete = new JSONObject();
					        delete.put("type", deleteroom);
					        delete.put("roomid", roomid);
					        delete.put("approved", "true");
					        out.writeUTF(delete.toJSONString());
					        out.flush();
					         					        
						}else{
							
							// if the client is not owner of the chat room
							// delete room unsuccessful
							JSONObject deleteroom = new JSONObject();
							deleteroom.put("type", "deleteroom");
							deleteroom.put("roomid", roomid);
							deleteroom.put("approved", "false");
							out.writeUTF(deleteroom.toJSONString());
							out.flush();
						}						
					}					
				}
			}catch(Exception e){
				e.printStackTrace();
			}			
		}
		
		
		//check the room name or identity name is valid
		private boolean validName(String name) {
			return name.matches("\\w+") && name.length() < 17 && name.length() > 2;
		}
		
		private boolean userDuplicateCheck(String name) {
			for (int i = 0; i < serverState.getUser().size(); i++) {
				if (name.equals(serverState.getUser().get(i))) {
					return false;
				}
			}
			return true;
		}
		
		//check the client is not the owner of another room 
		private boolean checkOwner(String identity){
			return !serverState.getOwnerToRoom().keySet().contains(identity);		
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

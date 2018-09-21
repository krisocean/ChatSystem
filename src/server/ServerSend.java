package server;

import java.io.DataOutputStream;
import java.net.Socket;
import org.json.simple.JSONObject;

public class ServerSend extends Thread {
		private JSONObject send;
		private ServerState serverState;
		public ServerSend(ServerState serverState, JSONObject send){
			try{
				this.serverState = serverState;
				this.send = send;
			}catch(Exception e){
				e.printStackTrace();
			}
			
		}
		
		public void run(){	
			try{		
				for(int i =0; i< serverState.getPortList().size(); i++){
						if(serverState.getPortList().get(i) != serverState.getCoordination_port()){
							Socket sendSocket = new Socket(serverState.getServerAddress(), serverState.getPortList().get(i));
							DataOutputStream serverOut = new DataOutputStream(sendSocket.getOutputStream());
							serverOut.writeUTF(send.toJSONString());
							serverOut.flush();
							sendSocket.close();				        
						}
					}					
			}catch(Exception e){
				e.printStackTrace();
			}
		}
}

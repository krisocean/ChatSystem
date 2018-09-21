package server;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class ReadConfig {
		
	    ArrayList<String> configLine = new ArrayList<String>();
		ArrayList<String> serverList = new ArrayList<String>();
		ArrayList<String> server_address = new ArrayList<String>();
		ArrayList<Integer> clientPorts = new ArrayList<Integer>();
		ArrayList<Integer> portList = new ArrayList<Integer>();
	
		public void fread(String server_conf) throws FileNotFoundException, IOException {
			try(BufferedReader br = new BufferedReader(new FileReader(server_conf))) {
			    for(String configLine; (configLine = br.readLine()) != null; ) {
			    	String[] configParams = configLine.split("\t");  			    	
			    	if(configParams.length == 4){
			    		serverList.add(configParams[0]);
					server_address.add(configParams[1]);
					clientPorts.add(Integer.parseInt(configParams[2]));
					portList.add(Integer.parseInt(configParams[3]));
					
			     	}
			    }
			}
		}
		
		public ArrayList<String> getServerList(){
			return serverList;
		}
		
		public ArrayList<String> getServer_address(){
			return server_address;
		}
		
		public ArrayList<Integer> getClientPorts(){
			return clientPorts;
		}
		
		public ArrayList<Integer> getPortList(){
			return portList;
		}
		
}

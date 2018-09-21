package server;

import org.kohsuke.args4j.Option;

public class CmdLineArgs {
		
	@Option(required = true, name ="-n", aliases ="--serverId", usage =" serverid")
	private String serverId;
	
	@Option(required = true, name = "-l", aliases = "--loadfile", usage = "server_conf")
	private String server_conf;
	
	public String getServerId(){
		return serverId;
	}
	
	public String getServer_conf(){
		return server_conf;
	}
}

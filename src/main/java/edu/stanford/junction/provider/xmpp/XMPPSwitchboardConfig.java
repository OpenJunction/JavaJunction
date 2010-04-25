package edu.stanford.junction.provider.xmpp;

import edu.stanford.junction.SwitchboardConfig;

public class XMPPSwitchboardConfig implements SwitchboardConfig {
	protected String host;
	protected String user;
	protected String password;
	
	public XMPPSwitchboardConfig(String host) {
		this.host = host;
	}
	
	public void setHost(String host) {
		this.host = host;
	}
	
	public void setCredentials(String username, String password) {
		this.user = username;
		this.password = password;
	}
	
	public XMPPSwitchboardConfig(){}
	
	protected String getChatService() {
		return "conference." + host;
	}
}
package edu.stanford.junction.provider.xmpp;

import edu.stanford.junction.SwitchboardConfig;

public class XMPPSwitchboardConfig implements SwitchboardConfig {
	protected String host;
	protected String user;
	protected String password;
	protected long connectionTimeout = 10000; // 10 seconds in milliseconds
	
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

	public void setConnectionTimeout(long t) {
		this.connectionTimeout = t;
	}
	
	public XMPPSwitchboardConfig(){}
	
	protected String getChatService() {
		return "conference." + host;
	}
}
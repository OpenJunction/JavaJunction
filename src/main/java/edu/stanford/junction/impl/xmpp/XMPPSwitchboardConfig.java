package edu.stanford.junction.impl.xmpp;

import edu.stanford.junction.SwitchboardConfig;

public class XMPPSwitchboardConfig implements SwitchboardConfig {
	protected String mHost;
	protected String mUser;
	protected String mPassword;
	
	public XMPPSwitchboardConfig(String host) {
		mHost = host;
	}
	
	public void setHost(String host) {
		mHost = host;
	}
	
	public void setCredentials(String username, String password) {
		mUser = username;
		mPassword = password;
	}
	
	public XMPPSwitchboardConfig(){}
}
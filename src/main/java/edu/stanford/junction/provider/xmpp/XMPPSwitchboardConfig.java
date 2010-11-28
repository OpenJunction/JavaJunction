/*
 * Copyright (C) 2010 Stanford University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package edu.stanford.junction.provider.xmpp;

import java.net.URI;

import edu.stanford.junction.SwitchboardConfig;

public class XMPPSwitchboardConfig implements SwitchboardConfig {
	protected String host;
	protected String user;
	protected String password;
	protected long connectionTimeout = 10000; // 10 seconds in milliseconds
	
	public XMPPSwitchboardConfig(URI uri) {
		this.host = uri.getAuthority();
	}
	
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
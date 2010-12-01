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


package edu.stanford.junction.provider.jx;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.UUID;

import org.json.JSONObject;

import edu.stanford.junction.Junction;
import edu.stanford.junction.api.activity.ActivityScript;
import edu.stanford.junction.api.activity.JunctionActor;
import edu.stanford.junction.api.messaging.MessageHeader;
import edu.stanford.junction.provider.jx.JXServer.Log;

/**
 * Implements Junction transport using a simple socket interface.
 *
 */
public class JunctionProvider extends edu.stanford.junction.provider.JunctionProvider {
	
	public JunctionProvider(JXSwitchboardConfig config) {
		
	}
	
	@Override
	public ActivityScript getActivityScript(URI uri) {
		JunctionActor actor = new JunctionActor("scriptpuller") {
			@Override
			public void onMessageReceived(MessageHeader header, JSONObject message) {
				
			}
		};

		Junction jx = new edu.stanford.junction.provider.jx.Junction(uri,null,actor);
		ActivityScript script = null;
		final int MAX_TIME = 10000; // ms
		final int WAIT = 300; // ms
		int total = 0;
		
		while (script == null && total < MAX_TIME) {
			try {
				Thread.sleep(WAIT);
			} catch (InterruptedException e) {}
			
			script = jx.getActivityScript();
			total += WAIT;
		}
		 
		actor.leave();
		return script;
	}

	@Override
	public Junction newJunction(URI uri, ActivityScript script, JunctionActor actor) {
		return new edu.stanford.junction.provider.jx.Junction(uri,script,actor);
	}

	@Override
	public URI generateSessionUri() {
		try {
			// Use local address as switchboard
			String sb = getLocalIpAddress();
			return new URI("junction://" + sb + "/" + UUID.randomUUID() + "#jx");
		} catch (Exception e) {
			throw new AssertionError("Invalid URI: " + e.getMessage());
		}
	}
	
	public static String getLocalIpAddress() {
	    try {
	        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
	            NetworkInterface intf = en.nextElement();
	            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
	                InetAddress inetAddress = enumIpAddr.nextElement();
	                if (!inetAddress.isLoopbackAddress()) {
	                    return inetAddress.getHostAddress().toString();
	                }
	            }
	        }
	    } catch (SocketException ex) {
	        Log.e("junction", ex.toString());
	    }
	    return null;
	}
}

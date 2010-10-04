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


package edu.stanford.junction.api.activity;

import java.util.HashMap;
import java.util.Map;

import edu.stanford.junction.Junction;
import edu.stanford.junction.JunctionException;
import edu.stanford.junction.JunctionMaker;
import edu.stanford.junction.provider.xmpp.XMPPSwitchboardConfig;

// TODO: This class requires a switchboard, and is probably XMPP specific. Fix accordingly.

public abstract class JunctionService extends JunctionActor {
	public static String SERVICE_CHANNEL="jxservice";
	
	private static Map<String,Junction>mJunctionMap;
	{
		mJunctionMap = new HashMap<String,Junction>();
	}
	
	String mRole;
	
	public abstract String getServiceName();
	
	public JunctionService() {
		super((String)null);
	}
	
	@Override
	public String[] getRoles() {
		return new String[] {mRole};
	}
	
	@Override
	public void onActivityStart() {}
	
	public final void register(String switchboard) {
		
		if (mJunctionMap.containsKey(switchboard)) return;
		
		ActivityScript desc = new ActivityScript();
		desc.setHost(switchboard);
		desc.setSessionID(SERVICE_CHANNEL);
		//desc.setActorID(getServiceName());
		desc.setActivityID("junction.service");
		
		// TODO: register(String sb) doesn't make sense any more.
		// Get rid of 'services' in general; just stick an actor to an activity.
		XMPPSwitchboardConfig config = new XMPPSwitchboardConfig(switchboard);
		JunctionMaker maker = (JunctionMaker) JunctionMaker.getInstance(config);

		try{
			Junction jx = maker.newJunction(desc, this);
			mJunctionMap.put(switchboard, jx);
		}
		catch(JunctionException e){
			System.err.println("Failed to register JunctionService");
			e.printStackTrace(System.err);
		}
	}
	
	public void setRole(String role) {
		mRole=role;
	}
}
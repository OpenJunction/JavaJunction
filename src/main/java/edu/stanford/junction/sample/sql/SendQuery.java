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


package edu.stanford.junction.sample.sql;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;


import edu.stanford.junction.Junction;
import edu.stanford.junction.JunctionMaker;
import edu.stanford.junction.api.activity.ActivityScript;
import edu.stanford.junction.api.activity.JunctionActor;
import edu.stanford.junction.api.messaging.MessageHeader;
import edu.stanford.junction.provider.xmpp.XMPPSwitchboardConfig;

public class SendQuery extends JunctionActor {
	
	public SendQuery() {
		super("sql-client");
	}
	
	public static void main(String[] argv) {
		try {
			// todo: same session for client/server
			System.out.println("Starting the query actor");
			
			ActivityScript activity = new ActivityScript();
			
			XMPPSwitchboardConfig config = new XMPPSwitchboardConfig("prpl.stanford.edu");
			JunctionMaker jm = JunctionMaker.getInstance(config);
			Junction jx = jm.newJunction(activity, new SendQuery());
			
			
			
			//Thread.sleep(5000);
			//callback.terminate();
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onActivityStart() {
		
		//System.out.println("Sending query: " + query.getQueryText());
	}

	@Override
	public void onMessageReceived(MessageHeader header, JSONObject message) {
		// TODO Auto-generated method stub
		
	}
}

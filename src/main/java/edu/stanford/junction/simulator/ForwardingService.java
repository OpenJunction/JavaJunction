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


package edu.stanford.junction.simulator;

import org.json.JSONObject;

import edu.stanford.junction.api.activity.JunctionService;
import edu.stanford.junction.api.messaging.MessageHandler;
import edu.stanford.junction.api.messaging.MessageHeader;

/**
 * This class allows a remote client to subscribe to a channel without having
 * to open a new connection for it. It may be useful for a singleton service 
 * expecting to connect to many activities.
 * @author bdodson
 *
 */
public class ForwardingService extends JunctionService {
	private String mChannel; // TODO: this needs to be on another server
	
	private ForwardingService() {}
	public static JunctionService newInstance() {
		return new ForwardingService();
	}
	
	@Override
	public String getServiceName() {
		return "JunctionMaker";
	}

	@Override
	public void onActivityStart() {
	}
	
	@Override
	public void onActivityJoin() {
	}

	@Override
	public void onMessageReceived(MessageHeader header, JSONObject message) {
		// TODO: get a new Junction for remote server
		// Figure out how to preserve sender info
		// ('originator' field or something?)
		//getJunction().sendMessageToChannel(mChannel, message)		
	}
	
}

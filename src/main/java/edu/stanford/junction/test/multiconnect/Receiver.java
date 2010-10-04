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


package edu.stanford.junction.test.multiconnect;

import java.net.URI;

import org.json.JSONObject;

import edu.stanford.junction.JunctionMaker;
import edu.stanford.junction.api.activity.JunctionActor;
import edu.stanford.junction.api.messaging.MessageHeader;

public class Receiver extends JunctionActor {
	String mName;
	public Receiver(String name) {
		super("receiver");
		mName=name;
	}

	
	@Override
	public void onMessageReceived(MessageHeader header, JSONObject inbound) {
		System.out.println(mName + " :: " + inbound);
		
		try {
			JSONObject msg = new JSONObject();
			msg.put("thanksFor", inbound.get("tic"));
			//header.getReplyTarget().sendMessage(msg);
			sendMessageToActor(header.getSender(),msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

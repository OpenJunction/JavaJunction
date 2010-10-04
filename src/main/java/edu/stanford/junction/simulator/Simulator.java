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

import org.json.JSONException;
import org.json.JSONObject;

import edu.stanford.junction.JunctionMaker;
import edu.stanford.junction.api.activity.ActivityScript;
import edu.stanford.junction.api.activity.JunctionActor;
import edu.stanford.junction.api.messaging.MessageHeader;
import edu.stanford.junction.provider.xmpp.XMPPSwitchboardConfig;

class simThread extends Thread{
	private int NumOfMessage;
	private int NumOfParticipant;
	private int sessionID;
	simThread(int NumMsg, int NumP, int id){
		NumOfMessage = NumMsg;
		NumOfParticipant = NumP;
		sessionID = id;
	}
	public void run(){
		ActivityScript desc = new ActivityScript();
		//String ad = "SimSession_" + sessionID;
		//desc.setSessionID(ad);
		JSONObject platform = new JSONObject();
		try{
			platform.put("android", "http://my.realsitic.url/for_android");
			desc.addRolePlatform("simulator", "android", platform);
		} catch (Exception e) {}
		XMPPSwitchboardConfig config = new XMPPSwitchboardConfig("prpl.stanford.edu");
		JunctionMaker maker = JunctionMaker.getInstance(config);
		for(int actor_i = NumOfParticipant-1 ; actor_i >=0; actor_i --){
			try{
				maker.newJunction(desc, new SimActor(NumOfMessage, actor_i));
			}
			catch(Exception e){
				e.printStackTrace(System.err);
			}
			//maker.newJunction(desc, new SimActor(NumOfMessage, 0));
		}
	}
}

public class Simulator {
	static int NumOfActivity = 1;
	static int NumOfMessage = 5;
	static int NumOfParticipant = 2;
	public static void main(String[] argv){
		for(int i = 0; i< NumOfActivity; i++){
			simThread st = new simThread(NumOfMessage, NumOfParticipant, i);
			st.start();
		}
		while(true) {
			try {
				Thread.sleep(500000);
			} catch (Exception e) {}
		}
	}
}

class SimActor extends JunctionActor{
	static Integer TotalMessage = 0;
	private int NumOfMessage;
	private int NumOfBouncedMessage = 0;
	private int ID;
	public SimActor(int numMsg, int _ID) {
		super("SimActor"+_ID);
		NumOfMessage = numMsg;
		if(NumOfMessage <= 0)
			NumOfMessage = 1;
		NumOfBouncedMessage = 0;
		ID = _ID;
	}
	@Override
	public void onActivityStart() {
		
	}
	@Override
	public void onActivityJoin() {
		if(ID == 0){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			JSONObject simMsg = new JSONObject();
			try {
				simMsg.put("service","simulation");
				simMsg.put("switchboard", "prpl.stanford.edu");
				simMsg.put("session", this.getJunction().getSessionID());
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			this.getJunction().sendMessageToSession(simMsg);
		}
	}
	
	public  void onMessageReceived(MessageHeader arg0, JSONObject arg1) {
		synchronized (TotalMessage){
			NumOfBouncedMessage++;
			TotalMessage++;
			System.out.println("TotalMessage: " + TotalMessage);
			if((NumOfBouncedMessage-1) < NumOfMessage){
				//System.out.print(ID + " received" + arg1+ " " + NumOfBouncedMessage + "\n");
				//this.getJunction().sendMessageToSession(arg1);
				this.sendMessageToSession(arg1);
				try {
					Thread.sleep(300);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}else{
				//this.leave();
			}
		}
	}
}
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


package edu.stanford.junction.props2;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.*;
import java.net.*;

import edu.stanford.junction.addon.JSONObjWrapper;
import edu.stanford.junction.api.activity.JunctionExtra;
import edu.stanford.junction.api.messaging.MessageHeader;

import edu.stanford.junction.JunctionException;
import edu.stanford.junction.JunctionMaker;
import edu.stanford.junction.Junction;
import edu.stanford.junction.SwitchboardConfig;
import edu.stanford.junction.api.activity.JunctionActor;
import edu.stanford.junction.api.activity.JunctionExtra;

public class PropDaemon extends Prop{
	
	static class AnyState implements IPropState{
		private String state;
		public AnyState(String state){
			this.state = state;
		}
		public IPropState applyOperation(JSONObject operation){ return this; }
		public JSONObject toJSON(){ 
			try{
				if(this.state == null){ 
					return new JSONObject("null"); 
				}
				else { 
					return new JSONObject(this.state); 
				}
			}
			catch(JSONException e){
				e.printStackTrace(System.err);
				return null;
			}
		}
		public IPropState copy(){ return new AnyState(this.state); }

		public String toString(){ return state; }

	}

	public PropDaemon(String propName, IPropState state, long seqNum){
		super(propName, state, seqNum);
	}

	public PropDaemon(String propName){
		this(propName, new AnyState(null), 0);
	}

	@Override
	protected IPropState reifyState(JSONObject obj){
		return new AnyState(obj.toString());
	}

	@Override
	public IProp newFresh(){
		return new PropDaemon(this.propName);
	}

	/**
	 * Ignore state operations.
	 */
	@Override
	protected void handleReceivedOp(JSONObject opMsg){
		logInfo("Ignoring operation.");
	}

	protected long syncInterval = 10000;
	private long lastSyncTime = 0;


	/**
	 * Slightly modify the behaviour of handleHello so that we 
	 * don't request sync on every hello message we receive.
	 *
	 */
	@Override
	protected void handleHello(JSONObject msg){
		if(!isSelfMsg(msg)){
			long t = (new Date()).getTime();
			long elapsedSinceLastSync = t - lastSyncTime;
			long seqNum = msg.optLong("localSeqNum");
			logInfo("Peer HELLO @ seqNum: " + seqNum);
			if(seqNum > this.sequenceNum && (elapsedSinceLastSync > syncInterval)) {
				enterSYNCMode();
				lastSyncTime = t;
			}
		}
		else{
			logInfo("Self HELLO");
		}
	}

	@Override
	protected long helloInterval(){
		return 5000;
	}

	@Override
	protected long syncRequestInterval(){
		return 10000;
	}

	/**
	 * Conveniance runner for PropDaemon
	 * Expects two arguments: junction-url propName
	 */
	public static void main(String[] args){
		if(args.length != 2){
			System.out.println("Usage: PROGRAM junction-url propName");
			System.exit(0);
		}
		final String urlStr = args[0];
		final String propName = args[1];

		JunctionActor actor = new JunctionActor("prop-daemon") {
				public void onActivityJoin() {
					System.out.println("Joined activity!");
				}
				public void onActivityCreate(){
					System.out.println("Created activity!");
				}
				public void onMessageReceived(MessageHeader header, JSONObject msg){
					System.out.println("Got message!");
				}
				public List<JunctionExtra> getInitialExtras(){
					ArrayList<JunctionExtra> l = new ArrayList<JunctionExtra>();
					l.add(new PropDaemon(propName));
					return l;
				}
			};

		try{
			URI uri = new URI(urlStr);
			SwitchboardConfig sb = JunctionMaker.getDefaultSwitchboardConfig(uri);
			JunctionMaker jxMaker = JunctionMaker.getInstance(sb);
			Junction jx = jxMaker.newJunction(uri, actor);
		}
		catch(URISyntaxException e){
			System.err.println("Failed to parse url!");
			e.printStackTrace(System.err);
		}
		catch(JunctionException e){
			System.err.println("Failed to connect to junction activity!");
			e.printStackTrace(System.err);
		}
		catch(Exception e){
			System.err.println("Failed to connect to junction activity!");
			e.printStackTrace(System.err);
		}
	}

}

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
import edu.stanford.junction.api.activity.JunctionExtra;
import edu.stanford.junction.api.messaging.MessageHeader;
import edu.stanford.junction.extra.JSONObjWrapper;

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
	protected void handleReceivedOp(JSONObject opMsg){}

	@Override
	protected int helloInterval(){
		return 5000;
	}

	@Override
	protected int syncRequestInterval(){
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
		String urlStr = args[0];
		String propName = args[1];
		
	}

}

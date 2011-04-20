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

import edu.stanford.junction.addon.JSONObjWrapper;
import edu.stanford.junction.api.activity.JunctionExtra;
import edu.stanford.junction.api.messaging.MessageHeader;


/**
 * Note: The use of 'synchronized' is very deliberate. 
 *
 * If you want to access the state of the prop, you must use
 * the withState callback, which serializes your state read
 * with respect to state changes resulting from addOperation or
 * handleMessage.
 *
 */

public abstract class Prop extends JunctionExtra implements IProp{
	protected static final int MODE_NORM = 1;
	protected static final int MODE_SYNC = 2;

	public static final int MSG_STATE_OPERATION = 1;
	public static final int MSG_WHO_HAS_STATE = 2;
	public static final int MSG_I_HAVE_STATE = 3;
	public static final int MSG_SEND_ME_STATE = 4;
	public static final int MSG_STATE_SYNC = 5;
	public static final int MSG_HELLO = 6;

	public static final String EVT_CHANGE = "change";
	public static final String EVT_SYNC = "sync";
	public static final String EVT_ANY = "*";

	private static boolean LOG_INFO = false;
	private static boolean LOG_ERROR = false;

	// Try to use Use zlib compression when sending/receiving state syncs
	protected static final boolean COMPRESS_STATE_SYNC = true;

	// Temporarily disable change notifications
	// for efficiency sometimes.
	protected boolean enableChangeEvents = true;

	protected String uuid = UUID.randomUUID().toString();
	protected String propName;
	protected String propReplicaName = "";

	protected IPropState state;
	protected IPropState cleanState;

	protected long sequenceNum = 0;
	protected String lastOpUUID = "";

	protected long staleness = 0;

	protected int mode = MODE_NORM;
	protected String syncId = "";

	// In sync mode, between the time when we send
	// WHO_HAS_STATE and when we exit sync mode, track
	// the highest seqId so far received via a I_HAVE_STATE
	// message. Send more than one SEND_ME_STATE message if 
	// we get better offers.
	protected long bestSyncSeqNum = -1;

	protected Vector<JSONObject> opsSYNC = new Vector<JSONObject>();

	protected Vector<JSONObject> pendingLocals = new Vector<JSONObject>();
	protected Vector<IPropChangeListener> changeListeners = new Vector<IPropChangeListener>();

	protected PropStats propStats = null;

	protected Timer taskTimer;
	protected boolean active = false;

	public Prop(String propName, String propReplicaName, IPropState state, long seqNum){
		this.propName = propName;
		this.cleanState = state;
		this.state = state.copy();
		this.sequenceNum = seqNum;
		this.propReplicaName = propReplicaName;
		taskTimer = new Timer();
		taskTimer.schedule(new PeriodicTask(), 0, 1000);
	}

	public Prop(String propName, String propReplicaName, IPropState state){
		this(propName, propReplicaName, state, 0);
	}

	public Prop(String propName, IPropState state, long seqNum){
		this(propName, propName + "-replica" + UUID.randomUUID().toString(), state, seqNum);
	}

	public Prop(String propName, IPropState state){
		this(propName, propName + "-replica" + UUID.randomUUID().toString(), state);
	}


	protected long timeOfLastSyncRequest = 0;
	protected long timeOfLastHello = 0;
	class PeriodicTask extends TimerTask{
		public void run(){
			long t = (new Date()).getTime();

			// Basic attempt to see if we're
			// connected (checking for null actor and
			// null junction).
			if(isActive()){

				// Ignore failures. These are not time-criticial
				// operations.
				try{
					if(mode == MODE_NORM){
						if((t - timeOfLastHello) > helloInterval()){
							sendHello();
						}
					}
					else if(mode == MODE_SYNC){
						if((t - timeOfLastSyncRequest) > syncRequestInterval()){
							broadcastSyncRequest();
						}
					}
				}
				catch(Exception e){
					e.printStackTrace(System.err);
				}
			}
		}
	}

	protected boolean isActive(){
		return active && getActor() != null && getActor().getJunction() != null;
	}

	protected long helloInterval(){
		return 3000;
	}

	protected long syncRequestInterval(){
		return 5000;
	}

	abstract public IProp newFresh();

	public IProp newKeepingListeners(){
		Prop p = (Prop)newFresh();
		Iterator<IPropChangeListener> it = changeListeners.iterator();
		while(it.hasNext()){
			p.addChangeListener(it.next());
		}
		return p;
	}

	public long getStaleness(){
		return this.staleness;
	}

	public long getSequenceNum(){
		return sequenceNum;
	}

	public PropStats getAndDisableStats(){
		PropStats tmp = propStats;
		propStats = null;
		return tmp;
	}

	public void startCollectingStats(){
		propStats = new PropStats();
	}

	synchronized public <T> T withState(IWithStateAction<T> action){
		return action.run(state);
	}

	public String stateToString(){
		return state.toString();
	}

	public JSONObject stateToJSON(){
		return state.toJSON();
	}

	public String getPropName(){
		return propName;
	}

	public String getPropReplicaName(){
		return propReplicaName;
	}

	protected void logInfo(String s){
		if (LOG_INFO) {
			System.out.println("prop@" + propReplicaName + ": " + s);
		}
	}

	protected void logErr(String s){
		if (LOG_ERROR) {
			System.err.println("prop@" + propReplicaName + ": " + s);
		}
	}

	protected void assertTrue(String s, boolean cond){
		if(!cond){
			logErr("ASSERTION FAILED: " + s);
		}
	}

	protected void logState(String s){
		if (!LOG_INFO) {
			return;
		}
		System.out.println("");
		System.out.println("--------");
		logInfo(s);
		System.out.println("pendingLocals: " + this.pendingLocals);
		System.out.println("opsSync: " + this.opsSYNC);
		System.out.println("sequenceNum: " + this.sequenceNum);
		System.out.println("-----------");
		System.out.println("");
	}

	abstract protected IPropState reifyState(JSONObject obj);

	
	synchronized public void addChangeListener(IPropChangeListener listener){
		changeListeners.add(listener);
	}

	synchronized public void removeChangeListener(IPropChangeListener listener){
		changeListeners.remove(listener);
	}

	synchronized public void removeChangeListenersOfType(String type){
		Iterator<IPropChangeListener> it = changeListeners.iterator();
		while(it.hasNext()){
			IPropChangeListener l = it.next();
			if(l.getType().equals(type)){
				it.remove();
			}
		}
	}

	synchronized public void removeAllChangeListeners(){
		changeListeners.clear();
	}

	synchronized protected void dispatchChangeNotification(String evtType, Object o){
		if(enableChangeEvents){
			for(IPropChangeListener l : changeListeners){
				if(l.getType().equals(evtType) || l.getType().equals(EVT_ANY)){
					l.onChange(o);
				}
			}
		}
	}

	/**
	 * Returns true if the normal event handling should proceed;
	 * Return false to stop cascading.
	 */
	@Override
	public boolean beforeOnMessageReceived(MessageHeader h, JSONObject m) {
		if(m.optString("propTarget").equals(this.getPropName())){
			try{
				m.put("senderActor", h.getSender());
				handleMessage(m);
				return false;
			}
			catch(Exception e){
				logErr("Error handling message: " + e);
			}
			return true;
		}
		else{
			return true; 
		}

	}


	// A helper utility.
	// TODO: Calling this over and over for long pendingLocals
	// buffers is inefficient.
	protected void removePendingLocal(JSONObject opMsg){
		Iterator<JSONObject> it = this.pendingLocals.iterator();
		String uuid = opMsg.optString("uuid");
		while(it.hasNext()){
			JSONObject m = it.next();
			if(m.optString("uuid").equals(uuid)){
				it.remove();

				/*** STATS INSTRUMENTATION ****/
				if(propStats != null){
					long t = System.nanoTime();
					long rttElapsed =  t - m.optLong("nanoTime");
					propStats.addMessageRTT(t, rttElapsed);
				}

			}
		}
	}

	/**
	 * What to do with a newly arrived operation? Depends on mode of 
	 * operation.
	 *
	 * Note, we must take care to make sure there is no sharing between
	 * this.cleanState and this.state. This means copying operations 
	 * that must be applied to both states!
	 */
	protected void handleReceivedOp(JSONObject opMsg){
		boolean changed = false;

		/*** STATS INSTRUMENTATION ****/
		if(propStats != null){
			propStats.addPredictionQLength(System.nanoTime(), this.pendingLocals.size());
		}

		if(isSelfMsg(opMsg)){
			// Received a confirmation of a message we already applied
			// locally. Apply to clean state.
			this.staleness = this.sequenceNum - opMsg.optLong("localSeqNum");
			this.cleanState.applyOperation(opMsg.optJSONObject("op"));

			// Get rid of the copy of the message in pending locals.
			removePendingLocal(opMsg);
		}
		else{
			if(this.pendingLocals.size() > 0){

				/*** STATS INSTRUMENTATION ****/
				if(propStats != null){
					propStats.addConflict(System.nanoTime(), this.pendingLocals.size());
				}

				// A remote message is received out-of-order with our predicted
				// local operations. Fix up the order.
				this.cleanState.applyOperation(opMsg.optJSONObject("op"));
				this.state = this.cleanState.copy();
				for(JSONObject msg : this.pendingLocals){
					this.state.applyOperation(msg.optJSONObject("op"));
				}
			}
			else{
				// We've no pending locals and we received a remote message.
				// Just apply it. cleanState and state should be equivalent.
				assertTrue("If pending locals is empty, state hash and cleanState hash should be equal.", 
						   this.state.hashCode() == this.cleanState.hashCode());
				JSONObject op = opMsg.optJSONObject("op");
				this.cleanState.applyOperation(op);
				this.state.applyOperation(JSONObjWrapper.copyJSONObject(op));
			}
			changed = true;
		}

		this.lastOpUUID = opMsg.optString("uuid");
		this.sequenceNum += 1;

		if(changed){
			this.dispatchChangeNotification(EVT_CHANGE, null);
		}

		this.logState("Got op off wire, finished processing: " + opMsg);
	}


	protected void exitSYNCMode(){
		logInfo("Exiting SYNC mode");
		this.mode = MODE_NORM;
		this.syncId = "";
		this.bestSyncSeqNum = -1;
	}

	protected void enterSYNCMode(){
		logInfo("Entering SYNC mode.");
		this.mode = MODE_SYNC;
		this.syncId = UUID.randomUUID().toString();
		this.sequenceNum = -1;
		this.opsSYNC.clear();
		this.bestSyncSeqNum = -1;
		broadcastSyncRequest();
	}

	synchronized protected void broadcastSyncRequest(){
		this.bestSyncSeqNum = -1;
		this.timeOfLastSyncRequest = (new Date()).getTime();
		sendMessageToProp(newWhoHasStateMsg(this.syncId));
	}

	protected boolean isSelfMsg(JSONObject msg){
		return msg.optString("senderReplicaUUID").equals(this.uuid);
	}

	synchronized protected void handleMessage(JSONObject msg){
		int msgType = msg.optInt("type");
		String fromActor = msg.optString("senderActor");
		switch(mode){
		case MODE_NORM:
			switch(msgType){
			case MSG_STATE_OPERATION: {
				handleReceivedOp(msg);
				break;
			}
			case MSG_SEND_ME_STATE: {
				if(!isSelfMsg(msg)){
					logInfo("Got SEND_ME_STATE");
					String syncId = msg.optString("syncId");
					String compression = msg.optString("compression");
					boolean compress = compression != null && compression.equals("zlib");
					sendMessageToPropReplica(fromActor, newStateSyncMsg(syncId, compress));
				}
				break;
			}
			case MSG_HELLO:{
				handleHello(msg);
				break;
			}
			case MSG_WHO_HAS_STATE:{
				if(!isSelfMsg(msg)){
					logInfo("Got WHO_HAS_STATE.");
					String syncId = msg.optString("syncId");
					sendMessageToPropReplica(fromActor, newIHaveStateMsg(syncId));
				}
				break;
			}
			default:
				logInfo("NORM mode: Ignoring message, "  + msg);
			}
			break;
		case MODE_SYNC:
			switch(msgType){
			case MSG_STATE_OPERATION:{
				this.opsSYNC.add(msg);
				logInfo("SYNC mode: buffering op..");
				break;
			}
			case MSG_I_HAVE_STATE:{
				String syncId = msg.optString("syncId");
				if(!isSelfMsg(msg) && syncId.equals(this.syncId)){
					long remoteSeqNum = msg.optLong("localSeqNum");
					if(remoteSeqNum > this.bestSyncSeqNum){
						logInfo("Best I_HAVE_STATE so far..");
						sendMessageToPropReplica(fromActor, newSendMeStateMsg(syncId));
						this.bestSyncSeqNum = remoteSeqNum;
					}
				}
				break;
			}
			case MSG_STATE_SYNC:{
				if(!isSelfMsg(msg)){
					// First check that this sync message corresponds to the current
					// SYNC mode...
					String syncId = msg.optString("syncId");
					if(!(syncId.equals(this.syncId))){
						logErr("Bogus sync id! ignoring StateSyncMsg");
					}
					else{
						// We may have send many SendMeState messages, so make sure we 
						// only use the best one...
						long remoteSeqNum = msg.optLong("seqNum");
						if(remoteSeqNum >= this.bestSyncSeqNum){
							logErr("Got desired state sync. Handling...");
							this.handleStateSyncMsg(msg);
						}
					}
				}
				break;
			}
			default:
				logInfo("SYNC mode: Ignoring message, "  + msg);
			}
		}
	}


	protected void handleHello(JSONObject msg){
		if(!isSelfMsg(msg)){
			long seqNum = msg.optLong("localSeqNum");
			logInfo("Peer HELLO @ seqNum: " + seqNum);
			if(seqNum > this.sequenceNum) {
				enterSYNCMode();
			}
		}
		else{
			logInfo("Self HELLO");
		}
	}


	/**
	 * Install state received from peer.
	 */
	protected void handleStateSyncMsg(JSONObject msg){
		logInfo("Got StateSyncMsg:" + msg);

		logInfo("Reifying received state..");
		String compression = msg.optString("compression");
		boolean compressed = compression != null && compression.equals("zlib");
		if(compressed){
			logInfo("Decompressing zlib compression...");
			String data = msg.optString("state");
			JSONObject state = JSONObjWrapper.expandCompressedObj(data);
			this.cleanState = this.reifyState(state);
		}
		else{
			logInfo("No compression...");
			JSONObject state = msg.optJSONObject("state");
			this.cleanState = this.reifyState(state);
		}
		logInfo("Copying clean to predicted..");
		this.state = this.cleanState.copy();
		this.sequenceNum = msg.optLong("seqNum");
		this.lastOpUUID = msg.optString("lastOpUUID");

		logInfo("Installed state.");
		logInfo("sequenceNum:" + this.sequenceNum);
		logInfo("Now applying buffered things....");

		//Scan the ops that we received while in SYNC mode.
		boolean apply = false;
		for(JSONObject m : this.opsSYNC){
			if(isSelfMsg(m)){
				/* If the op is a self op, we know it is incorporated
				   into the sync state ( it was send _before SYNC mode,
				   and it made it back to us, so the sync-supplier peer 
				   must have got it before generating the sync state). 

				   We use this op to clean up pending locals that were 
				   successfully broadcast before SYNC, but not confirmed 
				   before SYNC.*/
				removePendingLocal(m);
			}

			if(!apply && m.optString("uuid").equals(this.lastOpUUID)){
				apply = true;
				continue;
			}
			else if(apply && !isSelfMsg(m)){
				/* If the op is a peer op, we apply it, under the 
				   condition that it was _not_ already incorporated 
				   into the sync state. */
				handleReceivedOp(m);
			}
		}
		this.opsSYNC.clear();

		exitSYNCMode();


		/* At this point, pendingLocals includes _unconfirmed_ ops 
		   that were added locally _before_ entering sync mode 
		   (since we block addOperation during SYNC mode). That is,
		   ops that failed to propagate to the peers for some reason.
		   Perhaps the network was down. 
		   
		   We re-try each of the associated operations on top 
		   of the fresh sync state.
		*/
		enableChangeEvents = false;
		for(JSONObject m : this.pendingLocals){
			JSONObject op = m.optJSONObject("op");
			addOperation(op);
		}
		this.pendingLocals.clear();
		enableChangeEvents = true;

		logState("Finished syncing.");

		dispatchChangeNotification(EVT_SYNC, null);
	}


	/**
	 * Add an operation to the state managed by this Prop, with prediction
	 */
	synchronized public void addOperation(JSONObject operation){
		if(mode == MODE_NORM){
			logInfo("Adding predicted operation.");
			JSONObject msg = newStateOperationMsg(operation);
			sendMessageToProp(msg);
			this.state.applyOperation(operation);

			/*** STATS INSTRUMENTATION ****/
			if(propStats != null){
				try{
					msg.put("nanoTime", System.nanoTime());
				}catch(JSONException e){}
			}

			this.pendingLocals.add(msg);
			
			dispatchChangeNotification(EVT_CHANGE, operation);
		}
	}


	synchronized protected void sendHello(){
		this.timeOfLastHello = (new Date()).getTime();
		sendMessageToProp(newHelloMsg());
	}


	/**
	 * Send a message to all prop-replicas in this prop
	 */
	protected void sendMessageToProp(JSONObject m){
		try{
			m.put("propTarget", getPropName());
			m.put("senderReplicaUUID", uuid);
		}
		catch(JSONException e){
			logErr("JSON Error: " + e);
		}

		try{
			getActor().sendMessageToSession(m);
		}
		catch(Exception e){
			logErr("Failed to send to Prop.");
			e.printStackTrace(System.err);
		}
	}


	/**
	 * Send a message to the prop-replica hosted at the given actorId.
	 */
	protected void sendMessageToPropReplica(String actorId, JSONObject m){
		try{
			m.put("propTarget", getPropName());
			m.put("senderReplicaUUID", uuid);
		}
		catch(JSONException e){
			logErr("JSON Error: " + e);
		}

		try{
			getActor().sendMessageToActor(actorId, m);
		}
		catch(Exception e){
			logErr("Failed to send to Prop Replica.");
			e.printStackTrace(System.err);
		}

	}
    
	@Override
	public void afterActivityJoin() {
		this.active = true;
	}

	protected JSONObject newHelloMsg(){
		JSONObject m = new JSONObject();
		try{
			m.put("type", MSG_HELLO);
			m.put("localSeqNum", this.sequenceNum);
		}catch(JSONException e){
			logErr("JSON Error: " + e);
		}
		return m;
	}


	protected JSONObject newIHaveStateMsg(String syncId){
		JSONObject m = new JSONObject();
		try{
			m.put("type", MSG_I_HAVE_STATE);
			m.put("localSeqNum", this.sequenceNum);
			m.put("syncId", syncId);
		}catch(JSONException e){
			logErr("JSON Error: " + e);
		}
		return m;
	}

	protected JSONObject newWhoHasStateMsg(String syncId){
		JSONObject m = new JSONObject();
		try{
			m.put("type", MSG_WHO_HAS_STATE);
			m.put("syncId", syncId);
		}catch(JSONException e){
			logErr("JSON Error: " + e);
		}
		return m;
	}

	protected JSONObject newStateOperationMsg(JSONObject op){
		JSONObject m = new JSONObject();
		try{
			m.put("type", MSG_STATE_OPERATION);
			m.put("op", op);
			m.put("localSeqNum", this.sequenceNum);
			m.put("uuid", UUID.randomUUID().toString());
		}catch(JSONException e){
			logErr("JSON Error: " + e);
		}
		return m;
	}


	protected JSONObject newStateSyncMsg(String syncId, boolean compress){
		JSONObject m = new JSONObject();
		try{
			JSONObject state = this.cleanState.toJSON();
			if(compress){
				m.put("compression", "zlib");
				m.put("state", JSONObjWrapper.compressObj(state));
			}
			else{
				m.put("state", state);
			}
			m.put("type", MSG_STATE_SYNC);
			m.put("seqNum", this.sequenceNum);
			m.put("lastOpUUID", this.lastOpUUID);
			m.put("syncId", syncId);
		}catch(JSONException e){
			logErr("JSON Error: " + e);
		}
		return m;
	}

	protected JSONObject newSendMeStateMsg(String syncId){
		JSONObject m = new JSONObject();
		try{
			m.put("type", MSG_SEND_ME_STATE);
			m.put("syncId", syncId);
			if(COMPRESS_STATE_SYNC){
				m.put("compression", "zlib");
			}
		}catch(JSONException e){
			logErr("JSON Error: " + e);
		}
		return m;
	}

}

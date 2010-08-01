package edu.stanford.junction.props2;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.*;
import edu.stanford.junction.api.activity.JunctionExtra;
import edu.stanford.junction.api.messaging.MessageHeader;

public abstract class Prop extends JunctionExtra {
	private static final int MODE_NORM = 1;
	private static final int MODE_SYNC = 2;

	private static final int MSG_STATE_OPERATION = 1;
	private static final int MSG_WHO_HAS_STATE = 2;
	private static final int MSG_I_HAVE_STATE = 3;
	private static final int MSG_SEND_ME_STATE = 4;
	private static final int MSG_STATE_SYNC = 5;
	private static final int MSG_HELLO = 6;

	public static final String EVT_CHANGE = "change";
	public static final String EVT_SYNC = "sync";

	private String uuid = UUID.randomUUID().toString();
	private String propName;
	private String propReplicaName = "";

	private IPropState state;
	private IPropState cleanState;

	private long sequenceNum = 0;
	private String lastOpUUID = "";

	private long staleness = 0;

	private int mode = MODE_NORM;
	private String syncId = "";
	private boolean waitingForIHaveState = false;

	private Vector<JSONObject> opsSYNC = new Vector<JSONObject>();
	private Vector<JSONObject> stateSyncRequests = new Vector<JSONObject>();

	private Vector<JSONObject> pendingLocals = new Vector<JSONObject>();
	private Vector<IPropChangeListener> changeListeners = new Vector<IPropChangeListener>();

	private Timer taskTimer;
	private boolean active = false;

	public Prop(String propName, IPropState state, String propReplicaName){
		this.propName = propName;
		this.cleanState = state;	
		this.state = state.copy();
		this.propReplicaName = propReplicaName;
		taskTimer = new Timer();
		taskTimer.schedule(new PeriodicTask(), 0, 1000);
	}

	public Prop(String propName, IPropState state){
		this(propName, state, propName + "-replica" + UUID.randomUUID().toString());
	}


	private long timeOfLastSyncRequest = 0;
	private long timeOfLastHello = 0;
	class PeriodicTask extends TimerTask{
		public void run(){
			long t = (new Date()).getTime();
			if(active && getActor() != null && 
			   
			   // should be null if actor has 'left' the activity
			   getActor().getJunction() != null 
			   ){
				if(mode == MODE_NORM){
					if((t - timeOfLastHello) > 3000){
						sendHello();
					}
				}
				else if(mode == MODE_SYNC){
					if((t - timeOfLastSyncRequest) > 5000){
						broadcastSyncRequest();
					}
				}
			}
		}
	}

	public long getStaleness(){
		return this.staleness;
	}

	public long getSequenceNum(){
		return sequenceNum;
	}

	protected IPropState getState(){
		return state;
	}

	public String stateToString(){
		return state.toString();
	}

	public String getPropName(){
		return propName;
	}

	protected void logInfo(String s){
		System.out.println("prop@" + propReplicaName + ": " + s);
	}

	protected void logErr(String s){
		System.err.println("prop@" + propReplicaName + ": " + s);
	}

	protected void assertTrue(String s, boolean cond){
		if(!cond){
			logErr("ASSERTION FAILED: " + s);
		}
	}

	protected void logState(String s){
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

	public void addChangeListener(IPropChangeListener listener){
		changeListeners.add(listener);
	}

	protected void dispatchChangeNotification(String evtType, Object o){
		for(IPropChangeListener l : changeListeners){
			if(l.getType().equals(evtType)){
				l.onChange(o);
			}
		}
	}

	/**
	 * Returns true if the normal event handling should proceed;
	 * Return false to stop cascading.
	 */
	public boolean beforeOnMessageReceived(MessageHeader h, JSONObject m) {
		if(m.optString("propTarget").equals(this.getPropName())){
			try{
				m.put("senderActor", h.getSender());
				handleMessage(m);
				return false;
			}
			catch(JSONException e){
				logErr("JSON Error: " + e);
			}
			return true;
		}
		else{
			return true; 
		}

	}


	/**
	 * What to do with a newly arrived operation? Depends on mode of 
	 * operation.
	 */
	protected void handleReceivedOp(JSONObject opMsg){
		boolean changed = false;
		List<JSONObject> pendingLocals = new ArrayList<JSONObject>();
		if(isSelfMsg(opMsg)){
			this.staleness = this.sequenceNum - opMsg.optLong("localSeqNum");
			this.cleanState.applyOperation(opMsg.optJSONObject("op"));
			Iterator<JSONObject> it = this.pendingLocals.iterator();
			String uuid = opMsg.optString("uuid");
			while(it.hasNext()){
				JSONObject m = it.next();
				if(m.optString("uuid").equals(uuid)){
					it.remove();
				}
			}
		}
		else{
			if(this.pendingLocals.size() > 0){
				this.cleanState.applyOperation(opMsg.optJSONObject("op"));
				this.state = this.cleanState.copy();
				for(JSONObject msg : this.pendingLocals){
					this.state.applyOperation(msg.optJSONObject("op"));
				}
			}
			else{
				assertTrue("If pending locals is empty, state hash and cleanState hash should be equal.", 
						   this.state.hashCode() == this.cleanState.hashCode());
				this.cleanState.applyOperation(opMsg.optJSONObject("op"));
				this.state.applyOperation(opMsg.optJSONObject("op"));
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
		this.waitingForIHaveState = false;
	}

	protected void enterSYNCMode(){
		logInfo("Entering SYNC mode.");
		this.mode = MODE_SYNC;
		this.syncId = UUID.randomUUID().toString();
		this.sequenceNum = -1;
		this.opsSYNC.clear();
		this.waitingForIHaveState = true;
		broadcastSyncRequest();
	}

	synchronized protected void broadcastSyncRequest(){
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
					sendMessageToPropReplica(fromActor, newStateSyncMsg(syncId));
				}
				break;
			}
			case MSG_HELLO:{
				logInfo("Got HELLO.");
				if(!isSelfMsg(msg) && 
				   msg.optLong("localSeqNum") > this.sequenceNum) {
					enterSYNCMode();
				}
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
				if(!isSelfMsg(msg) && this.waitingForIHaveState && syncId.equals(this.syncId)){
					logInfo("Got I_HAVE_STATE.");
					sendMessageToPropReplica(fromActor, newSendMeStateMsg(syncId));
					this.waitingForIHaveState = false;
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
						this.handleStateSyncMsg(msg);
					}
				}
				break;
			}
			default:
				logInfo("SYNC mode: Ignoring message, "  + msg);
			}
		}
	}


	/**
	 * Install state received from peer.
	 */
	protected void handleStateSyncMsg(JSONObject msg){
		logInfo("Got StateSyncMsg:" + msg);

		logInfo("Reifying received state..");
		this.cleanState = this.reifyState(msg.optJSONObject("state"));
		logInfo("Copying clean to predicted..");
		this.state = this.cleanState.copy();
		this.sequenceNum = msg.optLong("seqNum");
		this.lastOpUUID = msg.optString("lastOpUUID");

		logInfo("Installed state.");
		logInfo("sequenceNum:" + this.sequenceNum);
		logInfo("Now applying buffered things....");

		// Forget all local predictions.
		this.pendingLocals.clear();

		// Apply any ops that we recieved while syncing,
		// ignoring those that are already incorporated 
		// into sync state.
		boolean apply = false;
		for(JSONObject m : this.opsSYNC){
			if(!apply && m.optString("uuid").equals(this.lastOpUUID)){
				apply = true;
				continue;
			}
			else if(apply){
				handleReceivedOp(m);
			}
		}
		this.opsSYNC.clear();
		exitSYNCMode();
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
			this.state.applyOperation(operation);
			dispatchChangeNotification(EVT_CHANGE, operation);
			this.pendingLocals.add(msg);
			sendMessageToProp(msg);
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
			getActor().sendMessageToSession(m);
		}catch(JSONException e){
			logErr("JSON Error: " + e);
		}
	}


	/**
	 * Send a message to the prop-replica hosted at the given actorId.
	 */
	protected void sendMessageToPropReplica(String actorId, JSONObject m){
		try{
			m.put("propTarget", getPropName());
			m.put("senderReplicaUUID", uuid);
			getActor().sendMessageToActor(actorId, m);
		}catch(JSONException e){
			logErr("JSON Error: " + e);
		}
	}
    
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


	protected JSONObject newStateSyncMsg(String syncId){
		JSONObject m = new JSONObject();
		try{
			m.put("type", MSG_STATE_SYNC);
			m.put("state", this.cleanState.toJSON());
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
		}catch(JSONException e){
			logErr("JSON Error: " + e);
		}
		return m;
	}

}

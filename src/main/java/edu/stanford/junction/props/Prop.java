package edu.stanford.junction.props;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.Vector;
import java.util.UUID;
import java.util.Random;
import edu.stanford.junction.api.activity.JunctionExtra;
import edu.stanford.junction.api.messaging.MessageHeader;

public abstract class Prop extends JunctionExtra {
	public static final int MODE_NORM = 1;
	public static final int MODE_SYNC = 2;
	public static final int MSG_STATE_OPERATION = 1;
	public static final int MSG_STATE_SYNC = 2;
	public static final int MSG_WHO_HAS_STATE = 3;
	public static final int MSG_I_HAVE_STATE = 4;
	public static final int MSG_SEND_ME_STATE = 5;
	public static final int MSG_PLZ_CATCHUP = 6;

	public static final int PLZ_CATCHUP_THRESHOLD = 5;

	private String uuid = UUID.randomUUID().toString();
	private String propName;
	private String propReplicaName = "";
	private IPropState state;
	private int mode = MODE_NORM;
	private int staleness = 0;
	private int stateNumber = 0;
	private long lastOperationNonce = 0;
	private long syncNonce = 0;
	private boolean waitingForIHaveState = false;
	private Vector<StateOperationMsg> incomingBuffer = new Vector<StateOperationMsg>();
	private Vector<HistoryMAC> verifyHistory = new Vector<HistoryMAC>();
	private Vector<IPropChangeListener> changeListeners = new Vector<IPropChangeListener>();


	public Prop(String propName, IPropState state, String propReplicaName){
		this.propName = propName;
		this.state = state;
		this.propReplicaName = propReplicaName;
	}

	public Prop(String propName, IPropState state){
		this(propName, state, propName + "-replica" + UUID.randomUUID().toString());
	}

	public int getStaleness(){
		return staleness;
	}

	public IPropState getState(){
		return state;
	}

	public int getStateNumber(){
		return stateNumber;
	}

	protected int getNextStateNumber(){
		return getStateNumber() + 1;
	}

	protected String getPropName(){
		return propName;
	}

	protected void signalStateUpdate(){}

	protected void logInfo(String s){
		System.out.println("prop@" + propReplicaName + ": " + s);
	}

	protected void logErr(String s){
		System.err.println("prop@" + propReplicaName + ": " + s);
	}

	protected HistoryMAC newHistoryMAC(){
		return new HistoryMAC(getStateNumber(), this.state.hash());
	}

	abstract protected IPropState destringifyState(String s);
	abstract protected IPropStateOperation destringifyOperation(String s);

	public void addChangeListener(IPropChangeListener listener){
		changeListeners.add(listener);
	}

	protected void dispatchChangeNotification(String evtType, Object o){
		for(IPropChangeListener l : changeListeners){
			logInfo("Dispatching change notification...");
			l.onChange(evtType, o);
		}
	}

	protected void applyOperation(StateOperationMsg msg, boolean notify){
		IPropStateOperation operation = msg.operation;
		this.state = state.applyOperation(operation);
		this.lastOperationNonce = operation.getNonce();
		this.stateNumber += 1;

		this.verifyHistory.insertElementAt(newHistoryMAC(), 0);
		Vector<HistoryMAC> newHistory = new Vector<HistoryMAC>();
		for(int i = 0; i < verifyHistory.size() && i < 10; i++){
			newHistory.add(verifyHistory.get(i));
		}
		this.verifyHistory = newHistory;

		if(notify && !(operation instanceof NullOp)){
			dispatchChangeNotification("change", null);
		}
	}

	protected void checkHistory(HistoryMAC mac){
		for(HistoryMAC m : this.verifyHistory){
			if(m.stateNumber == mac.stateNumber){
				if(!(m.stateHash.equals(mac.stateHash))){
					logErr("Invalid state!" + 
						   m + " vs " + 
						   mac + 
						   " broadcasting Hello to flush out newbs..");
					sendHello();
				}
			}
		}
	}

	protected void exitSYNCMode(){
		logInfo("Exiting SYNC mode");
		this.mode = MODE_NORM;
	}

	protected void enterSYNCMode(int desiredStateNumber){
		logInfo("Entering SYNC mode.");
		this.mode = MODE_SYNC;
		Random rng = new Random();
		this.syncNonce = rng.nextLong();
		sendMessageToProp(new WhoHasStateMsg(desiredStateNumber, this.syncNonce));
		this.waitingForIHaveState = true;
	}

	protected void handleMessage(MessageHeader header, JSONObject rawMsg){
		int msgType = rawMsg.optInt("type");
		String fromPeer = rawMsg.optString("senderReplicaUUID");
		String fromReplicaName = rawMsg.optString("senderReplicaName");
		logInfo(propReplicaName + " processing message from: " + fromReplicaName);
		String fromActor = header.getSender();
		boolean isSelfMsg = (fromPeer.equals(this.uuid));
		switch(mode){
		case MODE_NORM:
			switch(msgType){
			case MSG_STATE_OPERATION: {
				StateOperationMsg msg = new StateOperationMsg(rawMsg, this);
				IPropStateOperation operation = msg.operation;
				int predictedStateNumber = msg.predictedStateNumber;
				if(predictedStateNumber > getNextStateNumber()){
					// The predictedStateNumber of an operation is always <= 
					// it's true sequence num. So we check the predictedStateNumber 
					// against the # that would be it's true sequence num (this.stateSeqNum + 1),
					// were we to apply it.
					logInfo("Buffering FIRST " + 
							msg + "from " + 
							fromReplicaName + 
							". Currently at " + 
							getStateNumber());
					enterSYNCMode(predictedStateNumber - 1);
					this.incomingBuffer.add(msg);
				}
				else {
					// Note, operation may have been created WRT to stale state.
					//
					// It's not unsound to apply the operation - but we
					// hope that sender will eventually notice it's own
					// staleness and SYNC.

					// Send them a hint if things get too bad..
					if(!isSelfMsg && 
					   (getStateNumber() - predictedStateNumber) > PLZ_CATCHUP_THRESHOLD){
						logInfo("I'm at " + getStateNumber() + ", they're at " + predictedStateNumber + ". " + 
								"Sending catchup.");
						sendMessageToPropReplica(fromActor, new PlzCatchUpMsg(getStateNumber()));
					}

					if(isSelfMsg){
						this.staleness = (getNextStateNumber() - predictedStateNumber);
					}

					logInfo("Applying msg " + 
							msg + " from " + 
							fromReplicaName + 
							", currently at " + 
							getStateNumber());

					applyOperation(msg, true);

					if(!isSelfMsg){
						checkHistory(msg.mac);
					}
				}
				break;
			}
			case MSG_WHO_HAS_STATE:{
				WhoHasStateMsg msg = new WhoHasStateMsg(rawMsg);
				if(!isSelfMsg){
					// Can we fill the gap for this peer?
					if(getStateNumber() >= msg.desiredStateNumber){
						sendMessageToPropReplica(
							fromActor, 
							new IHaveStateMsg(getStateNumber(), msg.syncNonce));
					}
					else{
						logInfo("Oops! got state request for state i don't have!");
					}
				}
				break;
			}
			case MSG_SEND_ME_STATE:{
				SendMeStateMsg msg = new SendMeStateMsg(rawMsg);
				if(!isSelfMsg){
					logInfo("Got SendMeState");
					// Can we fill the gap for this peer?
					if(getStateNumber() > msg.desiredStateNumber){
						logInfo("Sending state..");
						sendMessageToPropReplica(
							fromActor, 
							new StateSyncMsg(
								getStateNumber(), 
								this.state.stringify(), 
								msg.syncNonce, 
								this.lastOperationNonce));
					}
				}
				break;
			}
			case MSG_PLZ_CATCHUP:{
				PlzCatchUpMsg msg = new PlzCatchUpMsg(rawMsg);
				if(!isSelfMsg){
					// Some peer is trying to tell us we are stale.
					// Do we believe them?
					logInfo("Got PlzCatchup : " + msg);
					if(msg.stateNumber > getStateNumber()) {
						enterSYNCMode(msg.stateNumber);
					}
				}
				break;
			}
			case MSG_STATE_SYNC:
				break;
			case MSG_I_HAVE_STATE:
				break;
			default:
				logErr("NORM mode: Unrecognized message, "  + rawMsg);
			}
			break;
		case MODE_SYNC:
			switch(msgType){
			case MSG_STATE_OPERATION:{
				StateOperationMsg msg = new StateOperationMsg(rawMsg, this);
				IPropStateOperation operation = msg.operation;
				int predictedStateNumber = msg.predictedStateNumber;
				if(predictedStateNumber <= getNextStateNumber() && this.incomingBuffer.isEmpty()){
					// We're in sync-mode, but it looks like we're up to date. 
					// Go back to NORM mode.
					logInfo("Applying msg " + msg + 
							" from " + fromReplicaName + 
							", currently at " + getStateNumber());
					applyOperation(msg, true);
					checkHistory(msg.mac);
					exitSYNCMode();
				}
				else{
					// Message is on far side of gap, buffer it.
					logInfo("Buffering " + msg + 
							" from " + fromReplicaName + 
							". Currently at " + getStateNumber());
					this.incomingBuffer.add(msg);
				}
				break;
			}
			case MSG_I_HAVE_STATE:{
				IHaveStateMsg msg = new IHaveStateMsg(rawMsg);
				if(!isSelfMsg){
					logInfo("Got IHaveState");
					if(msg.syncNonce == this.syncNonce && msg.stateNumber > getStateNumber()){
						logInfo("Requesting state");
						this.waitingForIHaveState = false;
						sendMessageToPropReplica(fromActor, new SendMeStateMsg(getStateNumber(), msg.syncNonce));
					}
				}
				break;
			}
			case MSG_STATE_SYNC:{
				StateSyncMsg msg = new StateSyncMsg(rawMsg);
				if(!isSelfMsg){
					// First check that this sync message corresponds to this
					// instance of SYNC mode. This is critical for assumptions
					// we make about the contents of incomingBuffer...
					if(msg.syncNonce != this.syncNonce){
						logInfo("Bogus SYNC nonce! ignoring StateSyncMsg");
					}
					else{
						logInfo("Got StateSyncMsg:" + msg.state);
	      
						// If local peer has buffered no operations, we know that no operations
						// were applied since the creation of state. We can safely assume
						// the given state.
						if(this.incomingBuffer.isEmpty()){
							logInfo("No buffered items.. applying sync");
							this.state = destringifyState(msg.state);
							this.lastOperationNonce = msg.lastOperationNonce;
							this.stateNumber = msg.stateNumber;
							exitSYNCMode();
							dispatchChangeNotification("change", null);
						}
						else{
							// Otherwise, we've buffered some operations.
							//
							// Since we started buffering before we sent the WhoHasState request, 
							// it must be that we've buffered all operations with seqNums >= that of
							// state.
							// 
							this.state = destringifyState(msg.state);
							this.lastOperationNonce = msg.lastOperationNonce;
							this.stateNumber = msg.stateNumber;

							// It's possible that ALL buffered messages are already incorportated
							// into state. In that case want to ignore buffered items and 
							// just assume state.

							// Otherwise, there is some tail of buffered operations that occurred after
							// the state was captured. We find this tail by comparing the nonce
							// of each buffered operation with that of the last operation incorporated
							// into state. (NOTE: we don't know the seqNums of the buffered msgs!
							// that's why we need the nonce.)
							for(int i = 0; i < this.incomingBuffer.size(); i++){
								StateOperationMsg m = this.incomingBuffer.get(i);
								if(m.operation.getNonce() == msg.lastOperationNonce){
									logInfo("Found nonce match in buffered messages!");
									for(int j = i + 1; j < this.incomingBuffer.size(); j++){
										applyOperation(this.incomingBuffer.get(j), false);
									}
									break;
								}
							}
							this.incomingBuffer.clear();
							exitSYNCMode();
							dispatchChangeNotification("change", null);
						}
					}
				}
				break;
			}
			default:
				logErr("SYNC mode: Unrecognized message, "  + rawMsg);
			}
			break;
		}

		signalStateUpdate();
	}

	/**
	 * Returns true if the normal event handling should proceed;
	 * Return false to stop cascading.
	 */
	public boolean beforeOnMessageReceived(MessageHeader h, JSONObject msg) { 
		if(msg.optString("propTarget").equals(getPropName())){
			handleMessage(h, msg);
			return false;
		}
		else{
			return true; 
		}
	}

	/**
	 * Send a hello message to all prop-replicas in this prop.
	 * The hello message is a state operation that does not affect
	 * the state. It serves to initiate conversation when all peers
	 * are at state 0.
	 */
	protected void sendHello(){
		int predictedStateNumber = getNextStateNumber();
		sendMessageToProp(new HelloMsg(
							  predictedStateNumber, 
							  getState().nullOperation(), 
							  newHistoryMAC()));
	}


	/**
	 * Send a message to all prop-replicas in this prop
	 */
	protected void sendOperation(IPropStateOperation operation){
		int predictedStateNumber = getNextStateNumber();
		HistoryMAC mac = newHistoryMAC();
		StateOperationMsg msg = new StateOperationMsg(
			predictedStateNumber, 
			operation, newHistoryMAC());
		logInfo(propReplicaName + 
				" sending " + msg + 
				". Current MAC: " + mac);
		sendMessageToProp(msg);
	}


	/**
	 * Add an operation to the state managed by this Prop
	 */
	synchronized public void addOperation(IPropStateOperation operation){
		sendOperation(operation);
	}


	/**
	 * Send a message to all prop-replicas in this prop
	 */
	protected void sendMessageToProp(PropMsg msg){
		JSONObject m = msg.toJSONObject();
		try{
			m.put("propTarget", getPropName());
			m.put("senderReplicaUUID", uuid);
			m.put("senderReplicaName", propReplicaName);
		}catch(JSONException e){}
		getActor().sendMessageToSession(m);
	}


	/**
	 * Send a message to the prop-replica hosted at the given actorId.
	 */
	protected void sendMessageToPropReplica(String actorId, PropMsg msg){
		JSONObject m = msg.toJSONObject();
		try{
			m.put("propTarget", getPropName());
			m.put("senderReplicaUUID", uuid);
			m.put("senderReplicaName", propReplicaName);
		}catch(JSONException e){}
		getActor().sendMessageToActor(actorId, m);
	}
    
	
	public void afterActivityJoin() {
		sendHello();
	}


	abstract class PropMsg{
		abstract public JSONObject toJSONObject();
	}

	class StateOperationMsg extends PropMsg{
		public int predictedStateNumber;
		public IPropStateOperation operation;
		public HistoryMAC mac;
		public StateOperationMsg(JSONObject msg, Prop prop){
			this.predictedStateNumber = msg.optInt("predStateNum");
			this.mac = new HistoryMAC(msg.optInt("macStateNum"),
									  msg.optString("macStateHash"));
			this.operation = prop.destringifyOperation(msg.optString("operation"));
		}
		public StateOperationMsg(int predictedStateNumber, IPropStateOperation operation, HistoryMAC mac){
			this.predictedStateNumber = predictedStateNumber;
			this.mac = mac;
			this.operation = operation;
		}
		public JSONObject toJSONObject(){

			JSONObject obj = new JSONObject();
			try{
				obj.put("type", MSG_STATE_OPERATION);
				obj.put("predStateNum", predictedStateNumber);
				obj.put("macStateNum", mac.stateNumber);
				obj.put("macStateHash", mac.stateHash);
				obj.put("operation", operation.stringify());
			}catch(JSONException e){}
			return obj;
		}
		public String toString(){
			return "[(" + (predictedStateNumber-1) + "->" 
				+ predictedStateNumber + ") " + 
				operation.getNonce() + "]";
		}
	}

	class HelloMsg extends StateOperationMsg{
		public HelloMsg(JSONObject msg, Prop prop){
			super(msg, prop);
		}
		public HelloMsg(int predictedStateNumber, IPropStateOperation operation, HistoryMAC mac){
			super(predictedStateNumber, operation, mac);
		}
	}

	class StateSyncMsg extends PropMsg{
		public int stateNumber;
		public String state;
		public long syncNonce;
		public long lastOperationNonce;
		public StateSyncMsg(JSONObject msg){
			stateNumber = msg.optInt("stateNumber");
			state = msg.optString("state");
			syncNonce = msg.optLong("syncNonce");
			lastOperationNonce = msg.optLong("lastOperationNonce");
		}
		public StateSyncMsg(int stateNumber, String state, long syncNonce, long lastOperationNonce){
			this.stateNumber = stateNumber;
			this.state = state;
			this.syncNonce = syncNonce;
			this.lastOperationNonce = lastOperationNonce;
		}
		public JSONObject toJSONObject(){
			JSONObject obj = new JSONObject();
			try{
				obj.put("type", MSG_STATE_SYNC);
				obj.put("stateNumber", stateNumber);
				obj.put("state", state);
				obj.put("syncNonce", syncNonce);
				obj.put("lastOperationNonce", lastOperationNonce);
			}catch(JSONException e){}
			return obj;
		}
	}

	class WhoHasStateMsg extends PropMsg{
		public int desiredStateNumber;
		public long syncNonce;
		public WhoHasStateMsg(JSONObject msg){
			desiredStateNumber = msg.optInt("desiredStateNumber");
			syncNonce = msg.optLong("syncNonce");
		}
		public WhoHasStateMsg(int desiredStateNumber, long syncNonce){
			this.desiredStateNumber = desiredStateNumber;
			this.syncNonce = syncNonce;
		}
		public JSONObject toJSONObject(){
			JSONObject obj = new JSONObject();
			try{
				obj.put("type", MSG_WHO_HAS_STATE);
				obj.put("desiredStateNumber", desiredStateNumber);
				obj.put("syncNonce", syncNonce);
			}catch(JSONException e){}
			return obj;
		}
	}


	class IHaveStateMsg extends PropMsg{
		public int stateNumber;
		public long syncNonce;
		public IHaveStateMsg(JSONObject msg){
			stateNumber = msg.optInt("stateNumber");
			syncNonce = msg.optLong("syncNonce");
		}
		public IHaveStateMsg(int stateNumber, long syncNonce){
			this.stateNumber = stateNumber;
			this.syncNonce = syncNonce;
		}
		public JSONObject toJSONObject(){
			JSONObject obj = new JSONObject();
			try{
				obj.put("type", MSG_I_HAVE_STATE);
				obj.put("stateNumber", stateNumber);
				obj.put("syncNonce", syncNonce);
			}catch(JSONException e){}
			return obj;
		}
	}

	class SendMeStateMsg extends PropMsg{
		public int desiredStateNumber;
		public long syncNonce;
		public SendMeStateMsg(JSONObject msg){
			desiredStateNumber = msg.optInt("desiredStateNumber");
			syncNonce = msg.optLong("syncNonce");
		}
		public SendMeStateMsg(int desiredStateNumber, long syncNonce){
			this.desiredStateNumber = desiredStateNumber;
			this.syncNonce = syncNonce;
		}
		public JSONObject toJSONObject(){
			JSONObject obj = new JSONObject();
			try{
				obj.put("type", MSG_SEND_ME_STATE);
				obj.put("desiredStateNumber", desiredStateNumber);
				obj.put("syncNonce", syncNonce);
			}catch(JSONException e){}
			return obj;
		}
	}

	class PlzCatchUpMsg extends PropMsg{
		public int stateNumber;
		public PlzCatchUpMsg(JSONObject msg){
			stateNumber = msg.optInt("stateNumber");
		}
		public PlzCatchUpMsg(int stateNumber){
			this.stateNumber = stateNumber;
		}
		public JSONObject toJSONObject(){
			JSONObject obj = new JSONObject();
			try{
				obj.put("type", MSG_PLZ_CATCHUP);
				obj.put("stateNumber", stateNumber);
			}catch(JSONException e){}
			return obj;
		}
	}


}

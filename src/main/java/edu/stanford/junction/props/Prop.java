package edu.stanford.junction.props;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.Vector;
import java.util.UUID;
import java.util.Random;
import edu.stanford.junction.api.activity.JunctionExtra;
import edu.stanford.junction.api.messaging.MessageHeader;


/**
 * TODO: 
 * state numbers should be long, not int
 *
 */
public abstract class Prop extends JunctionExtra {
	public static final int MODE_NORM = 1;
	public static final int MODE_SYNC = 2;
	public static final int MSG_STATE_OPERATION = 1;
	public static final int MSG_STATE_SYNC = 2;
	public static final int MSG_WHO_HAS_STATE = 3;
	public static final int MSG_I_HAVE_STATE = 4;
	public static final int MSG_SEND_ME_STATE = 5;
	public static final int MSG_PLZ_CATCHUP = 6;
	public static final int MSG_OP_ORDER_ACK = 7;

	public static final int PLZ_CATCHUP_THRESHOLD = 5;

	private String uuid = UUID.randomUUID().toString();
	private String propName;
	private String propReplicaName = "";

	private IPropState state;
	private IPropState finState;

	private long sequenceNum = 0;
	private long opSequenceNum = 0;

	private String lastOrderAckUUID = "";

	private int mode = MODE_NORM;
	private long staleness = 0;
	private long syncNonce = -1;
	private boolean waitingForIHaveState = false;

	private Vector<OperationOrderAckMsg> orderAckBuffer = new Vector<OperationOrderAckMsg>();
	private Vector<IStateOperationMsg> pendingLocals = new Vector<IStateOperationMsg>();
	private Vector<IStateOperationMsg> pendingNonLocals = new Vector<IStateOperationMsg>();
	private Vector<IStateOperationMsg> sequentialOpsBuffer = new Vector<IStateOperationMsg>();

	private Vector<HistoryMAC> verifyHistory = new Vector<HistoryMAC>();
	private Vector<IPropChangeListener> changeListeners = new Vector<IPropChangeListener>();

	public Prop(String propName, IPropState state, String propReplicaName){
		this.propName = propName;
		this.finState = state;
		this.state = finState.copy();
		this.propReplicaName = propReplicaName;
	}

	public Prop(String propName, IPropState state){
		this(propName, state, propName + "-replica" + UUID.randomUUID().toString());
	}

	public long getStaleness(){
		return staleness;
	}

	public IPropState getState(){
		return state;
	}

	public long getSequenceNum(){
		return opSequenceNum;
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
		return new HistoryMAC(opSequenceNum, this.finState.hash());
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



	/**
	 * Returns true if the normal event handling should proceed;
	 * Return false to stop cascading.
	 */
	public boolean beforeOnMessageReceived(MessageHeader h, JSONObject rawMsg) { 
		if(rawMsg.optString("propTarget").equals(getPropName())){
			IPropMsg msg = propMsgFromJSONObject(h, rawMsg, this);
			if(msg.getType() == MSG_STATE_OPERATION){
				IStateOperationMsg opMsg = (IStateOperationMsg)msg;

				logInfo("Got msg: " + opMsg);

				// Sort it into the buffer.
				Vector<IStateOperationMsg> buf = this.sequentialOpsBuffer;
				buf.add(opMsg);
				int len = buf.size();
				for(int i = 0; i < len; i++){
					IStateOperationMsg m = buf.get(i);
					if(opMsg.getSequenceNum() < m.getSequenceNum()){
						buf.add(i, buf.remove(len - 1));
						break;
					}
				}
				logInfo("Sorted op into seq buffer: " + sequentialOpsBuffer);
				processOperationsInSequence();
			}
			else{
				handleMessage(msg);
			}
			return false;
		}
		else{
			return true; 
		}
	}



	/**
	 * See 'Copies convergence in a distributed real-time collaborative environment' 2000 
	 *  Vidot, Cart, Ferrie, Suleiman
	 *
	 */
	protected void applyOperation(IStateOperationMsg msg, boolean notify, boolean localPrediction){
		IPropStateOperation op = msg.getOp();
		if(localPrediction){
			// apply predicted operation immediately
			logInfo("Applying local prediction: " + msg);
			this.state = state.applyOperation(op);

			if(notify){
				dispatchChangeNotification("change", null);
			}
		}
		else if(!(isSelfMsg(msg) && msg.isPredicted())){ // Broadcasts of our own local ops are ignored.
			logInfo("Applying remote message: " + msg);

			logInfo("Transforming remote op before application..");
			IPropStateOperation remoteOpT = msg.getOp();
			for(int i = 0; i < this.pendingLocals.size(); i++){
				IStateOperationMsg local = this.pendingLocals.get(i);
				IPropStateOperation localOp = local.getOp();

				IPropStateOperation localOpT = this.transposeForward(remoteOpT, localOp);
				this.pendingLocals.set(i, local.newWithOp(localOpT));

				remoteOpT = this.transposeForward(localOp, remoteOpT);
			}
			this.state = state.applyOperation(remoteOpT);
			this.finState = finState.applyOperation(remoteOpT);



			// Do some book-keeping for history debugging..
			this.verifyHistory.insertElementAt(newHistoryMAC(), 0);
			Vector<HistoryMAC> newHistory = new Vector<HistoryMAC>();
			for(int i = 0; i < verifyHistory.size() && i < 10; i++){
				newHistory.add(verifyHistory.get(i));
			}
			this.verifyHistory = newHistory;


			if(notify){
				dispatchChangeNotification("change", null);
			}
		}


		logInfo("pendingLocals: " + this.pendingLocals);
		logInfo("pendingNonLocals: " + this.pendingNonLocals);
		logInfo("orderAckBuffer: " + this.orderAckBuffer);
		logInfo("sequentialOpsBuffer: " + this.sequentialOpsBuffer);
		logInfo("opSequenceNum: " + this.opSequenceNum);
		logInfo("sequenceNum: " + this.sequenceNum);
		logInfo("");
	}


	/**
	 * Should return a new operation, defined on the state resulting from the execution of o1, 
	 * and realizing the same intention as op2.
	 */
	protected IPropStateOperation transposeForward(IPropStateOperation o1, IPropStateOperation o2){
		return o2;
	}

	protected void checkHistory(HistoryMAC mac){
		for(HistoryMAC m : this.verifyHistory){
			if(m.seqNum == mac.seqNum){
				if(!(m.stateHash.equals(mac.stateHash))){
					logErr("Invalid state!" + 
						   m + " vs " + 
						   mac + 
						   " broadcasting Hello to flush out newbs..");
				}
			}
		}
	}

	protected void exitSYNCMode(){
		logInfo("Exiting SYNC mode");
		this.mode = MODE_NORM;
		this.syncNonce = -1;
	}

	protected void enterSYNCMode(long desiredSeqNumber){
		logInfo("Entering SYNC mode.");
		this.mode = MODE_SYNC;
		Random rng = new Random();
		this.syncNonce = rng.nextLong();
		this.opSequenceNum = -1;
		this.sequenceNum = -1;
		sendMessageToProp(new WhoHasStateMsg(desiredSeqNumber, this.syncNonce));
		this.waitingForIHaveState = true;
	}

	protected void handleOrderAck(OperationOrderAckMsg msg){
		if(msg.opSequenceNum > opSequenceNum){
			logInfo("msg opSequenceNum is newer..");
			if(mode == MODE_NORM && !isSelfMsg(msg)){
				enterSYNCMode(msg.opSequenceNum);
			}
			
		}
		else if(msg.opSequenceNum < opSequenceNum){
			logInfo("msg opSequenceNum is older..");
			if(mode == MODE_NORM && !isSelfMsg(msg)){
				sendMessageToPropReplica(msg.getSenderActor(), new PlzCatchUpMsg(opSequenceNum));
			}
		}
		else{
			this.sequenceNum += 1;
			this.lastOrderAckUUID = msg.uuid;
			if(isSelfMsg(msg)){
				// When we get back the authoritative order for 
				// a message...
				if(msg.predicted){
					if(this.pendingLocals.size() > 0){
						IStateOperationMsg m = this.pendingLocals.get(0);
						if(m.getUUID().equals(msg.msgUUID)){
							m.setSequenceNum(this.sequenceNum);
							this.pendingLocals.remove(0);
							this.finState = finState.applyOperation(m.getOp());
							sendMessageToProp(m);
							logInfo("Ordered local prediction: " + m);
						}
						else{
							logErr("Got order Ack of local op out of order..uuid mismatch!!");
						}
					}
					else {
						logErr("Ack of local op found empty pendingLocals!!");
					}
				}
				else{
					if(this.pendingNonLocals.size() > 0){
						IStateOperationMsg m = this.pendingNonLocals.get(0);
						if(m.getUUID().equals(msg.msgUUID)){
							m.setSequenceNum(this.sequenceNum);
							this.pendingNonLocals.remove(0);
							sendMessageToProp(m);
							logInfo("Ordered non-local prediction: " + m);
						}
						else{
							logErr("Got Ack of non-local op out of order..uuid mismatch!!");
						}
					}
					else {
						logErr("Ack of non-local op found empty pendingNonLocals!!");
					}
				}
			}
		}
		logInfo("Order Ack handled:" + msg);
		logInfo("opSequenceNum:" + this.opSequenceNum);
		logInfo("sequenceNum:" + this.sequenceNum);
	}

	protected boolean isSelfMsg(IPropMsg msg){
		return msg.getSenderReplicaUUID().equals(this.uuid);
	}

	synchronized protected void handleMessage(IPropMsg rawMsg){
		int msgType = rawMsg.getType();
		String fromActor = rawMsg.getSenderActor();
		switch(mode){
		case MODE_NORM:
			switch(msgType){
			case MSG_STATE_OPERATION: {
				IStateOperationMsg msg = (IStateOperationMsg)rawMsg;
				applyOperation(msg, true, false);
				// if(msg.getFinStateNum() > finStateNum){
				// 	// Check whether the remote peer has more acknowledged state
				// 	// than we do..
				// 	logInfo("Buffering FIRST " + 
				// 			msg + "from " + 
				// 			msg.getSenderReplicaName() + 
				// 			". Currently at " + 
				// 			getStateNum());
				// 	enterSYNCMode(msg.getFinStateNum());
				// 	this.incomingBuffer.add(msg);
				// }
				// else {
				// 	// Note, operation may have been created WRT to stale state.
				// 	//
				// 	// It's not unsound to apply the operation - but we
				// 	// hope that sender will eventually notice it's own
				// 	// staleness and SYNC.

				// 	// Send them a hint if things get too bad..
				// 	if(!isSelfMsg(msg) && 
				// 	   (finStateNum - msg.getFinStateNum()) > PLZ_CATCHUP_THRESHOLD){
				// 		logInfo("I'm at " + finStateNum + ", they're at " + msg.getFinStateNum() + ". " + 
				// 				"Sending catchup.");
				// 		sendMessageToPropReplica(fromActor, new PlzCatchUpMsg(finStateNum));
				// 	}

				// 	if(isSelfMsg(msg)){
				// 		this.staleness = (finStateNum - msg.getFinStateNum());
				// 	}

				// 	applyOperation(msg, true, false);

				// 	if(!isSelfMsg(msg)){
				// 		checkHistory(msg.getHistoryMAC());
				// 	}
				// }
				break;
			}
			case MSG_WHO_HAS_STATE:{
				WhoHasStateMsg msg = (WhoHasStateMsg)rawMsg;
				if(!isSelfMsg(msg)){
					logInfo("Got who has state..");
					// Can we fill the gap for this peer?
					if(opSequenceNum >= msg.desiredSeqNumber){
						logInfo("Sending IHaveState..");
						sendMessageToPropReplica(
							fromActor, 
							new IHaveStateMsg(opSequenceNum, msg.syncNonce));
					}
					else{
						logInfo("Oops! got state request for state i don't have!");
					}
				}
				break;
			}
			case MSG_SEND_ME_STATE: {
				SendMeStateMsg msg = (SendMeStateMsg)rawMsg;
				if(!isSelfMsg(msg)){
					logInfo("Got SendMeState");
					// Can we fill the gap for this peer?
					if(opSequenceNum >= msg.desiredSeqNumber){
						StateSyncMsg sync = new StateSyncMsg(
							this.finState.stringify(),
							msg.syncNonce,
							this.opSequenceNum,
							this.sequenceNum,
							this.lastOrderAckUUID);
						logInfo("Sending state sync msg: " + sync);
						sendMessageToPropReplica(fromActor, sync);
					}
				}
				break;
			}
			case MSG_PLZ_CATCHUP:{
				PlzCatchUpMsg msg = (PlzCatchUpMsg)rawMsg;
				if(!isSelfMsg(msg)){
					// Some peer is trying to tell us we are stale.
					// Do we believe them?
					logInfo("Got PlzCatchup : " + msg);
					if(msg.seqNum > opSequenceNum) {
						enterSYNCMode(msg.seqNum);
					}
				}
				break;
			}
			case MSG_OP_ORDER_ACK:{
				OperationOrderAckMsg msg = (OperationOrderAckMsg)rawMsg;
				handleOrderAck(msg);
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
				logErr("Oops, shouldn't be processing a state operation in sync mode!");
				// IStateOperationMsg msg = (StateOperationMsg)rawMsg;
				// if(msg.getFinStateNum() <= finStateNum && this.incomingBuffer.isEmpty() && this.orderAckBuffer.isEmpty()){
				// 	// We're in sync-mode, but it looks like we're up to date. 
				// 	// Go back to NORM mode.
				// 	applyOperation(msg, true, false);
				// 	checkHistory(msg.getHistoryMAC());
				// 	exitSYNCMode();
				// }
				// else{
				// 	// Message is on far side of gap, buffer it.
				// 	logInfo("Buffering " + msg + 
				// 			" from " + msg.getSenderReplicaName() + 
				// 			". Currently acknowledged at " + finStateNum);
				// 	this.incomingBuffer.add(msg);
				// }
				break;
			}
			case MSG_I_HAVE_STATE:{
				IHaveStateMsg msg = (IHaveStateMsg)rawMsg;
				if(!isSelfMsg(msg) && this.waitingForIHaveState){
					logInfo("Got IHaveState");
					if(msg.syncNonce == this.syncNonce && msg.seqNum > opSequenceNum){
						logInfo("Requesting state");
						this.waitingForIHaveState = false;
						sendMessageToPropReplica(fromActor, new SendMeStateMsg(msg.seqNum, msg.syncNonce));
					}
				}
				break;
			}
			case MSG_OP_ORDER_ACK:{
				OperationOrderAckMsg msg = (OperationOrderAckMsg)rawMsg;
				this.orderAckBuffer.add(msg);
				logInfo("SYNC mode: buffering order ack..");
				break;
			}
			case MSG_STATE_SYNC:{
				StateSyncMsg msg = (StateSyncMsg)rawMsg;
				if(!isSelfMsg(msg)){
					// First check that this sync message corresponds to this
					// instance of SYNC mode. This is critical for assumptions
					// we make about the contents of incomingBuffer...
					if(msg.syncNonce != this.syncNonce){
						logInfo("Bogus SYNC nonce! ignoring StateSyncMsg");
					}
					else{
						logInfo("Got StateSyncMsg:" + msg);

						this.finState = destringifyState(msg.state);
						this.state = finState.copy();
						this.opSequenceNum = msg.opSequenceNum;
						this.sequenceNum = msg.sequenceNum;
						this.lastOrderAckUUID = msg.lastOrderAckUUID;

						logInfo("Installed state.");
						logInfo("opSequenceNum:" + opSequenceNum);
						logInfo("sequenceNum:" + sequenceNum);
						logInfo("Now applying buffered things....");

						// We may have applied some predictions locally.
						// Just forget all these predictions (we're wiping our
						// local state completely. Any straggler ACKS originating
						// from this peer will have to be ignored..
						this.pendingLocals.clear();

						// Also forget about any incomplete non-local ops..
						this.pendingNonLocals.clear();

						// Apply any sequence number updates that we've
						// received while syncing..
						boolean apply = false;
						for(OperationOrderAckMsg m : this.orderAckBuffer){
							if(!apply && m.uuid.equals(this.lastOrderAckUUID)){
								apply = true;
								continue;
							}
							else if(apply){
								handleOrderAck(m);	
							}
						}
						this.orderAckBuffer.clear();

						processOperationsInSequence();


						logInfo("Finished syncing.");
						logInfo("opSequenceNum:" + opSequenceNum);
						logInfo("sequenceNum:" + sequenceNum);

						exitSYNCMode();

						dispatchChangeNotification("sync", null);
					}
				}
				break;
			}
			}
		}

		signalStateUpdate();
	}


	protected void processOperationsInSequence(){
		// Execute all that are in sequence..
		// Recall that the sequence is always sorted
		// in ascending order of sequence number.
		Vector<IStateOperationMsg> buf = this.sequentialOpsBuffer;
		int i;
		int len = buf.size();
		for(i = 0; i < len; i++){
			IStateOperationMsg m = buf.get(i);
			if(m.getSequenceNum() < (opSequenceNum + 1)){
				logInfo("Skipping stale msg: " + i);
				// Continue..
				// We want to skip past all messages that are too
				// early (probably arrived during SYNC).
			}
			else if(m.getSequenceNum() == (opSequenceNum + 1)){
				this.opSequenceNum = m.getSequenceNum();
				logInfo("Handling msg at: " + i);
				handleMessage(m);
				// Continue..
				// There might be multiple to handle..
			}
			else if(m.getSequenceNum() > (opSequenceNum + 1)){
				break;
			}
		}

		// remove all that are handled or too early..
		buf.subList(0,i).clear();

		logInfo("Finished processing seq buffer: " + sequentialOpsBuffer);
	}

	/**
	 * Add an operation to the state managed by this Prop
	 */
	synchronized public void addOperation(IPropStateOperation operation){
		logInfo("Adding non-predicted operation.");
		HistoryMAC mac = newHistoryMAC();
		IStateOperationMsg msg = new StateOperationMsg(
			opSequenceNum + 1, 
			operation, 
			mac,
			false);
		this.pendingNonLocals.add(msg);
		OperationOrderAckMsg ack = new OperationOrderAckMsg(msg.getUUID(), false, opSequenceNum);
		sendMessageToProp(ack);
	}

	/**
	 * Add an operation to the state managed by this Prop, with prediction
	 */
	synchronized public void addPredictedOperation(IPropStateOperation operation){
		logInfo("Adding predicted operation.");
		HistoryMAC mac = newHistoryMAC();
		IStateOperationMsg msg = new StateOperationMsg(
			opSequenceNum + 1,
			operation,
			mac,
			true);
		applyOperation(msg, true, true);
		this.pendingLocals.add(msg);
		OperationOrderAckMsg ack = new OperationOrderAckMsg(msg.getUUID(), true, opSequenceNum);
		sendMessageToProp(ack);
	}


	/**
	 * Send a message to all prop-replicas in this prop
	 */
	protected void sendMessageToProp(IPropMsg msg){
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
	protected void sendMessageToPropReplica(String actorId, IPropMsg msg){
		JSONObject m = msg.toJSONObject();
		try{
			m.put("propTarget", getPropName());
			m.put("senderReplicaUUID", uuid);
			m.put("senderReplicaName", propReplicaName);
		}catch(JSONException e){}
		getActor().sendMessageToActor(actorId, m);
	}
    
	
	public void afterActivityJoin() {}

	public static class Pair<T, S>{
		private T first;
		private S second;
		public Pair(T f, S s){ 
			first = f;
			second = s;   
		}
		public T getFirst(){
			return first;
		}
		public S getSecond() {
			return second;
		}
		public String toString(){ 
			return "(" + first.toString() + ", " + second.toString() + ")"; 
		}
	}

	protected IPropMsg propMsgFromJSONObject(MessageHeader header, JSONObject obj, Prop prop){
		PropMsg msg = null;
		int type = obj.optInt("type");
		switch(type){
		case MSG_STATE_OPERATION:
			msg = new StateOperationMsg(obj, prop);
			break;
		case MSG_STATE_SYNC:
			msg = new StateSyncMsg(obj);
			break;
		case MSG_WHO_HAS_STATE:
			msg = new WhoHasStateMsg(obj);
			break;
		case MSG_I_HAVE_STATE:
			msg = new IHaveStateMsg(obj);
			break;
		case MSG_SEND_ME_STATE:
			msg = new SendMeStateMsg(obj);
			break;
		case MSG_PLZ_CATCHUP:
			msg = new PlzCatchUpMsg(obj);
			break;
		case MSG_OP_ORDER_ACK:
			msg = new OperationOrderAckMsg(obj);
			break;
		}
		if(msg != null){
			msg.senderActor = header.getSender();
		}
		else{
			logErr("Oh no! unknown message type: " + type);
		}
		return msg;
	}

	abstract class PropMsg implements IPropMsg{
		protected String senderReplicaUUID = "";
		protected String senderReplicaName = "";
		protected String senderActor = "";
		protected int type;

		public PropMsg(){}

		public PropMsg(JSONObject obj){
			this.type = obj.optInt("type");
			this.senderReplicaUUID = obj.optString("senderReplicaUUID");
			this.senderReplicaName = obj.optString("senderReplicaName");
		}

		abstract public JSONObject toJSONObject();

		public String toString(){
			return toJSONObject().toString();
		}

		public String getSenderReplicaUUID(){
			return senderReplicaUUID;
		}

		public String getSenderReplicaName(){
			return senderReplicaName;
		}

		public String getSenderActor(){
			return senderActor;
		}

		public int getType(){
			return type;
		}
	}

	class StateOperationMsg extends PropMsg implements IStateOperationMsg{
		protected long predictedSeqNum;
		protected long sequenceNum;
		protected String uuid;
		protected IPropStateOperation operation;
		protected HistoryMAC mac;
		protected boolean predicted;

		public StateOperationMsg(JSONObject msg, Prop prop){
			super(msg);
			this.uuid = msg.optString("uuid");
			this.predictedSeqNum = msg.optLong("predSeqNum");
			this.sequenceNum = msg.optLong("sequenceNum");
			this.predicted = msg.optBoolean("predicted");
			this.mac = new HistoryMAC(msg.optLong("macSeqNum"),
									  msg.optString("macStateHash"));
			this.operation = prop.destringifyOperation(msg.optString("operation"));
		}

		public StateOperationMsg(long predictedSeqNum, IPropStateOperation operation, HistoryMAC mac, boolean predicted){
			this.uuid = UUID.randomUUID().toString();
			this.predictedSeqNum = predictedSeqNum;
			this.sequenceNum = 0; // <- will be provided by MSG_OP_ORDER_ACK
			this.mac = mac;
			this.operation = operation;
			this.predicted = predicted;
		}

		public IStateOperationMsg newWithOp(IPropStateOperation op){
			StateOperationMsg msg = new StateOperationMsg(predictedSeqNum,
														  op,
														  mac,
														  predicted);
			msg.setSequenceNum(sequenceNum);
			return msg;
		}

		public boolean isPredicted(){
			return this.predicted;
		}
		public String getUUID(){
			return this.uuid;
		}
		public long getSequenceNum(){
			return this.sequenceNum;
		}
		public void setSequenceNum(long num){
			this.sequenceNum = num;
		}
		public long getPredictedSeqNum(){
			return this.predictedSeqNum;
		}
		public HistoryMAC getHistoryMAC(){
			return this.mac;
		}
		public IPropStateOperation getOp(){
			return this.operation;
		}

		public JSONObject toJSONObject(){

			JSONObject obj = new JSONObject();
			try{
				obj.put("type", MSG_STATE_OPERATION);
				obj.put("uuid", uuid);
				obj.put("sequenceNum", sequenceNum);
				obj.put("predSeqNum", predictedSeqNum);
				obj.put("macSeqNum", mac.seqNum);
				obj.put("macStateHash", mac.stateHash);
				obj.put("operation", operation.stringify());
				obj.put("predicted", predicted);
			}catch(JSONException e){}
			return obj;
		}

	}

	class HelloMsg extends StateOperationMsg{
		public HelloMsg(JSONObject msg, Prop prop){
			super(msg, prop);
		}
		public HelloMsg(long predictedSeqNumber, IPropStateOperation operation, HistoryMAC mac){
			super(predictedSeqNumber, operation, mac, false);
		}
	}

	class StateSyncMsg extends PropMsg{
		public String state;
		public long syncNonce;
		public long opSequenceNum;
		public long sequenceNum;
		public String lastOrderAckUUID;

		public StateSyncMsg(JSONObject msg){
			super(msg);
			state = msg.optString("state");
			syncNonce = msg.optLong("syncNonce");
			opSequenceNum = msg.optLong("opSeqNum");
			sequenceNum = msg.optLong("seqNum");
			lastOrderAckUUID = msg.optString("lastOrderAckUUID");
		}
		public StateSyncMsg(String state, long syncNonce, long opSeqNum, long seqNum, String lastOrderAckUUID){
			this.state = state;
			this.syncNonce = syncNonce;
			this.opSequenceNum = opSeqNum;
			this.sequenceNum = seqNum;
			this.lastOrderAckUUID = lastOrderAckUUID;
		}
		public JSONObject toJSONObject(){
			JSONObject obj = new JSONObject();
			try{
				obj.put("type", MSG_STATE_SYNC);
				obj.put("state", state);
				obj.put("syncNonce", syncNonce);
				obj.put("opSeqNum", opSequenceNum);
				obj.put("seqNum", sequenceNum);
				obj.put("lastOrderAckUUID", lastOrderAckUUID);
			}catch(JSONException e){}
			return obj;
		}

	}

	class WhoHasStateMsg extends PropMsg{
		public long desiredSeqNumber;
		public long syncNonce;
		public WhoHasStateMsg(JSONObject msg){
			super(msg);
			desiredSeqNumber = msg.optLong("desiredSeqNumber");
			syncNonce = msg.optLong("syncNonce");
		}
		public WhoHasStateMsg(long desiredSeqNumber, long syncNonce){
			this.desiredSeqNumber = desiredSeqNumber;
			this.syncNonce = syncNonce;
		}
		public JSONObject toJSONObject(){
			JSONObject obj = new JSONObject();
			try{
				obj.put("type", MSG_WHO_HAS_STATE);
				obj.put("desiredSeqNumber", desiredSeqNumber);
				obj.put("syncNonce", syncNonce);
			}catch(JSONException e){}
			return obj;
		}
	}


	class IHaveStateMsg extends PropMsg{
		public long seqNum;
		public long syncNonce;
		public IHaveStateMsg(JSONObject msg){
			super(msg);
			seqNum = msg.optLong("seqNum");
			syncNonce = msg.optLong("syncNonce");
		}
		public IHaveStateMsg(long seqNum, long syncNonce){
			this.seqNum = seqNum;
			this.syncNonce = syncNonce;
		}
		public JSONObject toJSONObject(){
			JSONObject obj = new JSONObject();
			try{
				obj.put("type", MSG_I_HAVE_STATE);
				obj.put("seqNum", seqNum);
				obj.put("syncNonce", syncNonce);
			}catch(JSONException e){}
			return obj;
		}
	}

	class SendMeStateMsg extends PropMsg{
		public long desiredSeqNumber;
		public long syncNonce;
		public SendMeStateMsg(JSONObject msg){
			super(msg);
			desiredSeqNumber = msg.optLong("desiredSeqNumber");
			syncNonce = msg.optLong("syncNonce");
		}
		public SendMeStateMsg(long desiredSeqNumber, long syncNonce){
			this.desiredSeqNumber = desiredSeqNumber;
			this.syncNonce = syncNonce;
		}
		public JSONObject toJSONObject(){
			JSONObject obj = new JSONObject();
			try{
				obj.put("type", MSG_SEND_ME_STATE);
				obj.put("desiredSeqNumber", desiredSeqNumber);
				obj.put("syncNonce", syncNonce);
			}catch(JSONException e){}
			return obj;
		}
	}

	class PlzCatchUpMsg extends PropMsg{
		public long seqNum;
		public PlzCatchUpMsg(JSONObject msg){
			super(msg);
			seqNum = msg.optLong("seqNum");
		}
		public PlzCatchUpMsg(long seqNum){
			this.seqNum = seqNum;
		}
		public JSONObject toJSONObject(){
			JSONObject obj = new JSONObject();
			try{
				obj.put("type", MSG_PLZ_CATCHUP);
				obj.put("seqNum", seqNum);
			}catch(JSONException e){}
			return obj;
		}
	}

	class OperationOrderAckMsg extends PropMsg{
		public String uuid;
		public String msgUUID;
		public boolean predicted;
		public long opSequenceNum;
		public OperationOrderAckMsg(JSONObject msg){
			super(msg);
			uuid = msg.optString("uuid");
			msgUUID = msg.optString("msgUUID");
			predicted = msg.optBoolean("predicted");
			opSequenceNum = msg.optInt("opSequenceNum");
		}
		public OperationOrderAckMsg(String msgUUID, boolean predicted, long opSequenceNum){
			this.uuid = UUID.randomUUID().toString();
			this.msgUUID = msgUUID;
			this.predicted = predicted;
			this.opSequenceNum = opSequenceNum;
		}
		public JSONObject toJSONObject(){
			JSONObject obj = new JSONObject();
			try{
				obj.put("type", MSG_OP_ORDER_ACK);
				obj.put("uuid", uuid);
				obj.put("msgUUID", msgUUID);
				obj.put("predicted", predicted);
				obj.put("opSequenceNum", opSequenceNum);
			}catch(JSONException e){}
			return obj;
		}
	}



}

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
 * don't say 'nonce', say 'uuid'
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
	private long stateNum = 0;
	private IPropState finState;
	private long finStateNum = 0;

	private long sequenceNum = 0;
	private long opSequenceNum = 0;

	private String lastOrderAckUUID = "";

	private int mode = MODE_NORM;
	private long staleness = 0;
	private long syncNonce = 0;
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

	public long getStateNum(){
		return stateNum;
	}

	protected long getNextStateNumber(){
		return getStateNum() + 1;
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
		return new HistoryMAC(finStateNum, this.finState.hash());
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
			this.stateNum += 1;

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
			this.stateNum += 1;

			this.finState = finState.applyOperation(remoteOpT);
			this.finStateNum += 1;



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
		logInfo("stateNum: " + this.stateNum);
		logInfo("finStateNum: " + this.finStateNum);
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
			if(m.stateNum == mac.stateNum){
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
		this.syncNonce = -1;
	}

	protected void enterSYNCMode(long desiredStateNumber){
		logInfo("Entering SYNC mode.");
		this.mode = MODE_SYNC;
		Random rng = new Random();
		this.syncNonce = rng.nextLong();
		this.opSequenceNum = -1;
		this.sequenceNum = -1;
		sendMessageToProp(new WhoHasStateMsg(desiredStateNumber, this.syncNonce));
		this.waitingForIHaveState = true;
	}

	protected void handleOrderAck(OperationOrderAckMsg msg){ 
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
						this.finStateNum += 1;
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
					if(finStateNum >= msg.desiredStateNumber){
						logInfo("Sending IHaveState..");
						sendMessageToPropReplica(
							fromActor, 
							new IHaveStateMsg(finStateNum, msg.syncNonce));
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
					if(finStateNum >= msg.desiredStateNumber){
						StateSyncMsg sync = new StateSyncMsg(
							finStateNum, 
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
					if(msg.finStateNum > finStateNum) {
						enterSYNCMode(msg.finStateNum);
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
					if(msg.syncNonce == this.syncNonce && msg.finStateNum > finStateNum){
						logInfo("Requesting state");
						this.waitingForIHaveState = false;
						sendMessageToPropReplica(fromActor, new SendMeStateMsg(msg.finStateNum, msg.syncNonce));
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
						logInfo("Got StateSyncMsg:" + msg.state);


						this.finState = destringifyState(msg.state);
						this.finStateNum = msg.finStateNum;
						this.state = finState.copy();
						this.stateNum = finStateNum;
						this.opSequenceNum = msg.opSequenceNum;
						this.sequenceNum = msg.sequenceNum;
						this.lastOrderAckUUID = msg.lastOrderAckUUID;

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

						dispatchChangeNotification("change", null);
						exitSYNCMode();
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
	public boolean beforeOnMessageReceived(MessageHeader h, JSONObject rawMsg) { 
		if(rawMsg.optString("propTarget").equals(getPropName())){
			IPropMsg msg = propMsgFromJSONObject(h, rawMsg, this);
			if(msg.getType() == MSG_STATE_OPERATION){
				IStateOperationMsg opMsg = (IStateOperationMsg)msg;

				logInfo("Got msg: " + opMsg);
				logInfo("opSequenceNum = " + opSequenceNum);

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
				// Continue..
				// We want to skip past all messages that are too
				// early (probably arrived during SYNC).
			}
			else if(m.getSequenceNum() == (opSequenceNum + 1)){
				handleMessage(m);
				this.opSequenceNum = m.getSequenceNum();
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
	 * Send a hello message to all prop-replicas in this prop.
	 * The hello message is a state operation that does not affect
	 * the state. It serves to initiate conversation when all peers
	 * are at state 0.
	 */
	protected void sendHello(){
		sendMessageToProp(new HelloMsg(
							  getNextStateNumber(), 
							  finStateNum,
							  new NullOp(), 
							  newHistoryMAC()));
	}


	/**
	 * Add an operation to the state managed by this Prop
	 */
	synchronized public void addOperation(IPropStateOperation operation){
		HistoryMAC mac = newHistoryMAC();
		IStateOperationMsg msg = new StateOperationMsg(
			getNextStateNumber(), 
			finStateNum,
			operation, 
			mac,
			false);
		this.pendingNonLocals.add(msg);
		OperationOrderAckMsg ack = new OperationOrderAckMsg(msg.getUUID(), false);
		sendMessageToProp(ack);
	}

	/**
	 * Add an operation to the state managed by this Prop, with prediction
	 */
	synchronized public void addPredictedOperation(IPropStateOperation operation){
		HistoryMAC mac = newHistoryMAC();
		IStateOperationMsg msg = new StateOperationMsg(
			getNextStateNumber(),
			finStateNum,
			operation,
			mac,
			true);
		applyOperation(msg, true, true);
		this.pendingLocals.add(msg);
		OperationOrderAckMsg ack = new OperationOrderAckMsg(msg.getUUID(), true);
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
    
	
	public void afterActivityJoin() {
		sendHello();
	}


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
		protected long predictedStateNum;
		protected long finStateNum;
		protected long sequenceNum;
		protected String uuid;
		protected IPropStateOperation operation;
		protected HistoryMAC mac;
		protected boolean predicted;

		public StateOperationMsg(JSONObject msg, Prop prop){
			super(msg);
			this.uuid = msg.optString("uuid");
			this.predictedStateNum = msg.optLong("predStateNum");
			this.finStateNum = msg.optLong("finStateNum");
			this.sequenceNum = msg.optLong("sequenceNum");
			this.predicted = msg.optBoolean("predicted");
			this.mac = new HistoryMAC(msg.optLong("macStateNum"),
									  msg.optString("macStateHash"));
			this.operation = prop.destringifyOperation(msg.optString("operation"));
		}

		public StateOperationMsg(long predictedStateNum, long finStateNum, IPropStateOperation operation, HistoryMAC mac, boolean predicted){
			this.uuid = UUID.randomUUID().toString();
			this.predictedStateNum = predictedStateNum;
			this.finStateNum = finStateNum;
			this.sequenceNum = -1; // <- will be provided by MSG_OP_ORDER_ACK
			this.mac = mac;
			this.operation = operation;
			this.predicted = predicted;
		}

		public IStateOperationMsg newWithOp(IPropStateOperation op){
			StateOperationMsg msg = new StateOperationMsg(predictedStateNum,
														  finStateNum,
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
		public long getFinStateNum(){
			return this.finStateNum;
		}
		public long getSequenceNum(){
			return this.sequenceNum;
		}
		public void setSequenceNum(long num){
			this.sequenceNum = num;
		}
		public long getPredictedStateNum(){
			return this.predictedStateNum;
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
				obj.put("finStateNum", finStateNum);
				obj.put("sequenceNum", sequenceNum);
				obj.put("predStateNum", predictedStateNum);
				obj.put("macStateNum", mac.stateNum);
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
		public HelloMsg(long predictedStateNumber, long finStateNum, IPropStateOperation operation, HistoryMAC mac){
			super(predictedStateNumber, finStateNum, operation, mac, false);
		}
	}

	class StateSyncMsg extends PropMsg{
		public long finStateNum;
		public String state;
		public long syncNonce;
		public long opSequenceNum;
		public long sequenceNum;
		public String lastOrderAckUUID;

		public StateSyncMsg(JSONObject msg){
			super(msg);
			finStateNum = msg.optLong("finStateNum");
			state = msg.optString("state");
			syncNonce = msg.optLong("syncNonce");
			opSequenceNum = msg.optLong("opSeqNum");
			sequenceNum = msg.optLong("seqNum");
			lastOrderAckUUID = msg.optString("lastOrderAckUUID");
		}
		public StateSyncMsg(long finStateNum, String state, long syncNonce, long opSeqNum, long seqNum, String lastOrderAckUUID){
			this.finStateNum = finStateNum;
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
				obj.put("finStateNum", finStateNum);
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
		public long desiredStateNumber;
		public long syncNonce;
		public WhoHasStateMsg(JSONObject msg){
			super(msg);
			desiredStateNumber = msg.optLong("desiredStateNumber");
			syncNonce = msg.optLong("syncNonce");
		}
		public WhoHasStateMsg(long desiredStateNumber, long syncNonce){
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
		public long finStateNum;
		public long syncNonce;
		public IHaveStateMsg(JSONObject msg){
			super(msg);
			finStateNum = msg.optLong("finStateNum");
			syncNonce = msg.optLong("syncNonce");
		}
		public IHaveStateMsg(long finStateNum, long syncNonce){
			this.finStateNum = finStateNum;
			this.syncNonce = syncNonce;
		}
		public JSONObject toJSONObject(){
			JSONObject obj = new JSONObject();
			try{
				obj.put("type", MSG_I_HAVE_STATE);
				obj.put("finStateNum", finStateNum);
				obj.put("syncNonce", syncNonce);
			}catch(JSONException e){}
			return obj;
		}
	}

	class SendMeStateMsg extends PropMsg{
		public long desiredStateNumber;
		public long syncNonce;
		public SendMeStateMsg(JSONObject msg){
			super(msg);
			desiredStateNumber = msg.optLong("desiredStateNumber");
			syncNonce = msg.optLong("syncNonce");
		}
		public SendMeStateMsg(long desiredStateNumber, long syncNonce){
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
		public long finStateNum;
		public PlzCatchUpMsg(JSONObject msg){
			super(msg);
			finStateNum = msg.optLong("finStateNum");
		}
		public PlzCatchUpMsg(long finStateNum){
			this.finStateNum = finStateNum;
		}
		public JSONObject toJSONObject(){
			JSONObject obj = new JSONObject();
			try{
				obj.put("type", MSG_PLZ_CATCHUP);
				obj.put("finStateNum", finStateNum);
			}catch(JSONException e){}
			return obj;
		}
	}

	class OperationOrderAckMsg extends PropMsg{
		public String uuid;
		public String msgUUID;
		public boolean predicted;
		public OperationOrderAckMsg(JSONObject msg){
			super(msg);
			uuid = msg.optString("uuid");
			msgUUID = msg.optString("msgUUID");
			predicted = msg.optBoolean("predicted");
		}
		public OperationOrderAckMsg(String msgUUID, boolean predicted){
			this.uuid = UUID.randomUUID().toString();
			this.msgUUID = msgUUID;
			this.predicted = predicted;
		}
		public JSONObject toJSONObject(){
			JSONObject obj = new JSONObject();
			try{
				obj.put("type", MSG_OP_ORDER_ACK);
				obj.put("uuid", uuid);
				obj.put("msgUUID", msgUUID);
				obj.put("predicted", predicted);
			}catch(JSONException e){}
			return obj;
		}
	}



}

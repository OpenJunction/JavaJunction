package edu.stanford.junction.props;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.Vector;
import java.util.Iterator;
import java.util.UUID;
import java.util.Random;
import edu.stanford.junction.api.activity.JunctionExtra;
import edu.stanford.junction.api.messaging.MessageHeader;



public abstract class Prop extends JunctionExtra {
	private static final int MODE_NORM = 1;
	private static final int MODE_SYNC = 2;
	private static final int MSG_STATE_OPERATION = 1;
	private static final int MSG_STATE_SYNC = 2;
	private static final int MSG_WHO_HAS_STATE = 3;
	private static final int MSG_I_HAVE_STATE = 4;
	private static final int MSG_SEND_ME_STATE = 5;
	private static final int MSG_PLZ_CATCHUP = 6;
	private static final int MSG_OP_ORDER_ACK = 7;
	private static final int MSG_HELLO = 8;

	public static final String EVT_CHANGE = "change";
	public static final String EVT_SYNC = "sync";

	private String uuid = UUID.randomUUID().toString();
	private String propName;
	private String propReplicaName = "";

	private IPropState state;

	private long seqNumCounter = 0;
	private long sequenceNum = 0;

	private String lastOrderAckUUID = "";
	private String lastOpUUID = "";

	private int mode = MODE_NORM;
	private long staleness = 0;
	private long syncNonce = -1;
	private boolean waitingForIHaveState = false;

	private Vector<OperationOrderAckMsg> orderAckSYNC = new Vector<OperationOrderAckMsg>();
	private Vector<IStateOperationMsg> opsSYNC = new Vector<IStateOperationMsg>();

	private Vector<SendMeStateMsg> stateSyncRequests = new Vector<SendMeStateMsg>();

	private Vector<IStateOperationMsg> pendingLocals = new Vector<IStateOperationMsg>();
	private Vector<IStateOperationMsg> sequentialOpsBuffer = new Vector<IStateOperationMsg>();


	private Vector<IPropChangeListener> changeListeners = new Vector<IPropChangeListener>();

	public Prop(String propName, IPropState state, String propReplicaName){
		this.propName = propName;
		this.state = state;
		this.propReplicaName = propReplicaName;
	}

	public Prop(String propName, IPropState state){
		this(propName, state, propName + "-replica" + UUID.randomUUID().toString());
	}

	public long getStaleness(){
		return staleness;
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

	protected void die(String s){
		System.err.println("prop@" + propReplicaName + ": " + s);
		System.err.flush();
		System.exit(1);
	}

	protected void logState(String s){
		System.out.println("");
		System.out.println("--------");
		logInfo(s);
		System.out.println("pendingLocals: " + this.pendingLocals);
		System.out.println("orderAckSYNC: " + this.orderAckSYNC);
		System.out.println("opsSync: " + this.opsSYNC);
		System.out.println("sequentialOpsBuffer: " + this.sequentialOpsBuffer);
		System.out.println("sequenceNum: " + this.sequenceNum);
		System.out.println("seqNumCounter: " + this.seqNumCounter);
		System.out.println("-----------");
		System.out.println("");
	}

	abstract protected IPropState destringifyState(String s);
	abstract protected IPropStateOperation destringifyOperation(String s);

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
	public boolean beforeOnMessageReceived(MessageHeader h, JSONObject rawMsg) {
		if(rawMsg.optString("propTarget").equals(getPropName())){
			IPropMsg msg = propMsgFromJSONObject(h, rawMsg, this);
			handleMessage(msg);
			return false;
		}
		else{
			return true; 
		}

	}


	/**
	 * What to do with a newly arrived operation? Depends on mode of 
	 * operation.
	 */
	private void handleReceivedOp(IStateOperationMsg opMsg){
		this.lastOpUUID = opMsg.getUUID();
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

		// Process any messages that are ready..
		processIncomingOpsSequentially();
		
		// Send out any pending broadcasts of predicted operations..
		processDeferredBroadcasts();

		// Check if state is calm, to process any pending state sync requests..
		processStateSyncRequests();
		logState("Got op off wire, finished processing: " + opMsg);
	}


	/**
	 * Process as many state ops as possible.
	 */
	private void processIncomingOpsSequentially(){
		// Recall that the sequence is always sorted
		// in ascending order of sequence number.
		Vector<IStateOperationMsg> buf = this.sequentialOpsBuffer;
		int i;
		int len = buf.size();

		// Proposal:
		// If we're stuck waiting for a particular message,
		// forget it after some threshold.
		// Note, this decision MUST be the same at all replicas!
		if(len > 10){
			logErr("sequentialOpsBuffer buffer too long! All replicas to next message!");
			this.sequenceNum = buf.get(0).getSequenceNum() - 1;
		}

		for(i = 0; i < len; i++){
			IStateOperationMsg m = buf.get(i);
			if(m.getSequenceNum() < (sequenceNum + 1)){
				// We want to discard messages that are too early.
				// Decrement the sequence number counter, since we're not using that sequence num..
				this.seqNumCounter -= 1;
				logErr("Decrementing seqNumCounter, and ignoring: " + m);
			}
			else if(m.getSequenceNum() == (sequenceNum + 1)){
				this.sequenceNum = m.getSequenceNum();
				applyOperation(m, true, false);
				logInfo("Sequentially processed: " + m.getSequenceNum());
				// There might be multiple to handle..
			}
			else if(m.getSequenceNum() > (sequenceNum + 1)){
				break;
			}
		}
		buf.subList(0,i).clear();
	}


	
	/**
	 * Wait for a moment of calm to send out state synchronization messages.
	 * Otherwise we would have to serialize all these buffers and send as part
	 * of the sync.
	 *
	 * Question: Is it realistic to expect these all to be empty at some times?
	 */
	private void processStateSyncRequests(){
		if(this.sequentialOpsBuffer.isEmpty() && 
		   this.pendingLocals.isEmpty()){
			for(SendMeStateMsg msg : this.stateSyncRequests){
				StateSyncMsg sync = new StateSyncMsg(
					this.state.stringify(),
					msg.syncNonce,
					this.sequenceNum,
					this.seqNumCounter,
					this.lastOrderAckUUID,
					this.lastOpUUID);
				sendMessageToPropReplica(msg.getSenderActor(), sync);
			}
			this.stateSyncRequests.clear();
		}
	}


	
	/**
	 * The order ack tells us the sequence number for the 
	 * corresponding message. Set the sequence number.
	 *
	 * deferredSend will broadcast the message once all msgs
	 * with smaller sequence numbers have been handled.
	 */
	private void handleOrderAck(OperationOrderAckMsg msg){
		// Is this a safe assumption?
		if(msg.sequenceNum > sequenceNum){
			logState("Ignoring order ack that's too new:" + msg);
			logInfo("msg sequenceNum is newer: " + msg.sequenceNum);
			if(mode == MODE_NORM && !isSelfMsg(msg)){
				enterSYNCMode(msg.sequenceNum);
				this.orderAckSYNC.add(msg);
			}
		}
		else{
			this.seqNumCounter += 1;
			this.lastOrderAckUUID = msg.uuid;
			if(!isSelfMsg(msg)){
				logState("Acknowledging peer's order ack: " + msg);
			}
			else{
				
				// When we get back the authoritative order for 
				// a message...
				boolean found = false;
				Iterator<IStateOperationMsg> it = this.pendingLocals.iterator();
				while(it.hasNext()){
					IStateOperationMsg m = it.next();
					if(m.getUUID().equals(msg.msgUUID)){
						m.setSequenceNum(this.seqNumCounter);
						logState("Ordered local prediction: " + m);
						found = true;
						break;
					}
				}
				
				if(!found){
					logErr("Ack of local op could not find pending op!!");
				}
			}
		}
		processDeferredBroadcasts();
		processStateSyncRequests();
	}

	/**
	 * Broadcasts of predicted ops are deferred until the local 
	 * sequence number indicates that all messages with lesser
	 * sequence numbers have been processed.
	 */
	private void processDeferredBroadcasts(){
		// Note. Messages should be sorted in ascending order of sequenceNumber
		Iterator<IStateOperationMsg> it = this.pendingLocals.iterator();
		while(it.hasNext()){
			IStateOperationMsg m = it.next();
			if(m.getSequenceNum() <= (sequenceNum + 1) && 
			   (m.getSequenceNum() != StateOperationMsg.NO_SEQ_NUM)){
				sendMessageToProp(m);
				it.remove();
				logInfo("Broadcast deferred op: " + m.getSequenceNum());
			}
		}
	}


	/**
	 * See 'Copies convergence in a distributed real-time collaborative environment' 2000 
	 *  Vidot, Cart, Ferrie, Suleiman
	 *
	 */
	private void applyOperation(IStateOperationMsg msg, boolean notify, boolean localPrediction){
		IPropStateOperation op = msg.getOp();
		if(localPrediction){
			// apply predicted operation immediately
			this.state = state.applyOperation(op);
			if(notify){
				dispatchChangeNotification(EVT_CHANGE, op);
			}
		}
		else if(!isSelfMsg(msg)){ // Broadcasts of our own local ops are ignored.
			try{
				IPropStateOperation remoteOpT = msg.getOp();
				for(int i = 0; i < this.pendingLocals.size(); i++){
					IStateOperationMsg local = this.pendingLocals.get(i);
					IPropStateOperation localOp = local.getOp();

					IPropStateOperation localOpT = this.transposeForward(remoteOpT, localOp);
					this.pendingLocals.set(i, local.newWithOp(localOpT));

					remoteOpT = this.transposeForward(localOp, remoteOpT);
				}
				this.state = state.applyOperation(remoteOpT);
			}
			catch(UnexpectedOpPairException e){
				logErr(" --- STATE IS CORRUPT! ---  " + e);
			}

			if(notify){
				dispatchChangeNotification(EVT_CHANGE, msg.getOp());
			}
		}

	}

	/**
	 * Assume o1 and o2 operate on the same state s.
	 * 
	 * Intent Preservation:
	 * transposeForward(o1,o2) is a new operation, defined on the state resulting from the execution of o1, 
	 * and realizing the same intention as op2.
	 * 
	 * Convergence:
	 * It must hold that o1*transposeForward(o1,o2) = o2*transposeForward(o2,o1).
	 *
	 * (where oi*oj denotes the execution of oi followed by the execution of oj)
	 * 
	 */
	protected IPropStateOperation transposeForward(IPropStateOperation o1, IPropStateOperation o2) throws UnexpectedOpPairException{
		return o2;
	}

	private void exitSYNCMode(){
		logInfo("Exiting SYNC mode");
		this.mode = MODE_NORM;
		this.syncNonce = -1;
		this.waitingForIHaveState = false;
	}

	private void enterSYNCMode(long desiredSeqNumber){
		logInfo("Entering SYNC mode.");
		this.mode = MODE_SYNC;
		Random rng = new Random();
		this.syncNonce = rng.nextLong();
		this.sequenceNum = -1;
		this.seqNumCounter = -1;
		this.orderAckSYNC.clear();
		this.opsSYNC.clear();
		this.sequentialOpsBuffer.clear();
		sendMessageToProp(new WhoHasStateMsg(desiredSeqNumber, this.syncNonce));
		this.waitingForIHaveState = true;
	}

	private boolean isSelfMsg(IPropMsg msg){
		return msg.getSenderReplicaUUID().equals(this.uuid);
	}

	synchronized private void handleMessage(IPropMsg rawMsg){
		int msgType = rawMsg.getType();
		String fromActor = rawMsg.getSenderActor();
		switch(mode){
		case MODE_NORM:
			switch(msgType){
			case MSG_STATE_OPERATION: {
				IStateOperationMsg msg = (IStateOperationMsg)rawMsg;
				handleReceivedOp(msg);
				break;
			}
			case MSG_WHO_HAS_STATE:{
				WhoHasStateMsg msg = (WhoHasStateMsg)rawMsg;
				if(!isSelfMsg(msg)){
					// Can we fill the gap for this peer?
					if(sequenceNum >= msg.desiredSeqNumber){
						sendMessageToPropReplica(
							fromActor, 
							new IHaveStateMsg(sequenceNum, msg.syncNonce));
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
					// Can we fill the gap for this peer?
					if(sequenceNum >= msg.desiredSeqNumber){
						this.stateSyncRequests.add(msg);
						this.processStateSyncRequests();
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
					if(msg.seqNum > sequenceNum) {
						enterSYNCMode(msg.seqNum);
					}
				}
				break;
			}
			case MSG_HELLO:{
				HelloMsg msg = (HelloMsg)rawMsg;
				if(msg.seqNum < sequenceNum) {
					sendMessageToPropReplica(
						fromActor,
						new PlzCatchUpMsg(sequenceNum));
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
				IStateOperationMsg msg = (IStateOperationMsg)rawMsg;
				if(!isSelfMsg(msg)){
					this.opsSYNC.add(msg);
					logInfo("SYNC mode: buffering op..");
				}
				else{
					logInfo("SYNC mode: ignoring self op..");
				}
				break;
			}
			case MSG_I_HAVE_STATE:{
				IHaveStateMsg msg = (IHaveStateMsg)rawMsg;
				if(!isSelfMsg(msg) && this.waitingForIHaveState){
					if(msg.syncNonce == this.syncNonce && msg.seqNum > sequenceNum){
						this.waitingForIHaveState = false;
						sendMessageToPropReplica(fromActor, new SendMeStateMsg(msg.seqNum, msg.syncNonce));
					}
				}
				break;
			}
			case MSG_OP_ORDER_ACK:{
				OperationOrderAckMsg msg = (OperationOrderAckMsg)rawMsg;
				if(!isSelfMsg(msg)){
					this.orderAckSYNC.add(msg);
					logInfo("SYNC mode: buffering order ack..");
				}
				else{
					logInfo("SYNC mode: ignoring self order ack..");
				}
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

						this.state = destringifyState(msg.state);
						this.sequenceNum = msg.sequenceNum;
						this.seqNumCounter = msg.seqNumCounter;
						this.lastOrderAckUUID = msg.lastOrderAckUUID;
						this.lastOpUUID = msg.lastOpUUID;

						logInfo("Installed state.");
						logInfo("sequenceNum:" + sequenceNum);
						logInfo("seqNumCounter:" + seqNumCounter);
						logInfo("Now applying buffered things....");

						// We may have applied some predictions locally.
						// Just forget all these predictions (we're wiping our
						// local state completely. Any straggler ACKS originating
						// from this peer will have to be ignored..
						this.pendingLocals.clear();

						// Apply any ordering acknowledgements that 
						// we recieved while syncing. Ignore those that are
						// already incorporated into sync state.
						boolean apply = false;
						for(OperationOrderAckMsg m : this.orderAckSYNC){
							if(!apply && m.uuid.equals(this.lastOrderAckUUID)){
								apply = true;
								continue;
							}
							else if(apply){
								handleOrderAck(m);
							}
						}
						this.orderAckSYNC.clear();


						// Apply any ops that we recieved while syncing,
						// ignoring those that are incorporated into sync state.
						apply = false;
						for(IStateOperationMsg m : this.opsSYNC){
							if(!apply && m.getUUID().equals(this.lastOpUUID)){
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
				}
				break;
			}
			}
		}
	}


	/**
	 * Add an operation to the state managed by this Prop, with prediction
	 */
	synchronized public void addOperation(IPropStateOperation operation){
		if(mode == MODE_NORM){
			logInfo("Adding predicted operation.");
			IStateOperationMsg msg = new StateOperationMsg(
				operation,
				true);
			applyOperation(msg, true, true);
			this.pendingLocals.add(msg);
			OperationOrderAckMsg ack = new OperationOrderAckMsg(msg.getUUID(), true, sequenceNum);
			logState("Requesting order ack: " + ack);
			sendMessageToProp(ack);
		}
	}


	/**
	 * Send a message to all prop-replicas in this prop
	 */
	private void sendMessageToProp(IPropMsg msg){
		JSONObject m = msg.toJSONObject();
		try{
			m.put("propTarget", getPropName());
			m.put("senderReplicaUUID", uuid);
		}catch(JSONException e){
			logErr("JSON Error: " + e);
		}
		getActor().sendMessageToSession(m);
	}


	/**
	 * Send a message to the prop-replica hosted at the given actorId.
	 */
	private void sendMessageToPropReplica(String actorId, IPropMsg msg){
		JSONObject m = msg.toJSONObject();
		try{
			m.put("propTarget", getPropName());
			m.put("senderReplicaUUID", uuid);
		}catch(JSONException e){
			logErr("JSON Error: " + e);
		}
		getActor().sendMessageToActor(actorId, m);
	}
    
	
	public void afterActivityJoin() {
		sendMessageToProp(new HelloMsg(sequenceNum));
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

	private IPropMsg propMsgFromJSONObject(MessageHeader header, JSONObject obj, Prop prop){
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
		case MSG_HELLO:
			msg = new HelloMsg(obj);
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
		protected String senderActor = "";
		protected int type;

		public PropMsg(){}

		public PropMsg(JSONObject obj){
			this.type = obj.optInt("type");
			this.senderReplicaUUID = obj.optString("senderReplicaUUID");
		}

		abstract public JSONObject toJSONObject();

		public String toString(){
			return toJSONObject().toString();
		}

		public String getSenderReplicaUUID(){
			return senderReplicaUUID;
		}

		public String getSenderActor(){
			return senderActor;
		}

		public int getType(){
			return type;
		}
	}

	class StateOperationMsg extends PropMsg implements IStateOperationMsg{
		public static final int NO_SEQ_NUM = -1;
		protected long sequenceNum = NO_SEQ_NUM;
		protected String uuid;
		protected IPropStateOperation operation;
		protected boolean predicted;

		public StateOperationMsg(JSONObject msg, Prop prop){
			super(msg);
			this.uuid = msg.optString("uuid");
			this.sequenceNum = msg.optLong("seqNum");
			this.predicted = msg.optBoolean("pred");
			this.operation = prop.destringifyOperation(msg.optString("op"));
		}

		public StateOperationMsg(String uuid, IPropStateOperation operation, boolean predicted){
			this.uuid = uuid;
			this.operation = operation;
			this.predicted = predicted;
		}

		public StateOperationMsg(IPropStateOperation operation, boolean predicted){
			this(UUID.randomUUID().toString(), operation, predicted);
		}

		public IStateOperationMsg newWithOp(IPropStateOperation op){
			StateOperationMsg msg = new StateOperationMsg(uuid,
														  op,
														  predicted);
			msg.setSequenceNum(sequenceNum);
			return msg;
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

		public IPropStateOperation getOp(){
			return this.operation;
		}

		public JSONObject toJSONObject(){

			JSONObject obj = new JSONObject();
			try{
				obj.put("type", MSG_STATE_OPERATION);
				obj.put("uuid", uuid);
				obj.put("seqNum", sequenceNum);
				obj.put("op", operation.stringify());
				obj.put("pred", predicted);
			}catch(JSONException e){}
			return obj;
		}

	}


	class StateSyncMsg extends PropMsg{
		public String state;
		public long syncNonce;
		public long sequenceNum;
		public long seqNumCounter;
		public String lastOrderAckUUID;
		public String lastOpUUID;

		public StateSyncMsg(JSONObject msg){
			super(msg);
			state = msg.optString("state");
			syncNonce = msg.optLong("syncNonce");
			sequenceNum = msg.optLong("opSeqNum");
			seqNumCounter = msg.optLong("seqNumCounter");
			lastOrderAckUUID = msg.optString("lastOrderAckUUID");
			lastOpUUID = msg.optString("lastOpUUID");
		}
		public StateSyncMsg(String state, long syncNonce, long opSeqNum, long seqNumCounter, String lastOrderAckUUID, String lastOpUUID){
			this.state = state;
			this.syncNonce = syncNonce;
			this.sequenceNum = opSeqNum;
			this.seqNumCounter = seqNumCounter;
			this.lastOrderAckUUID = lastOrderAckUUID;
			this.lastOpUUID = lastOpUUID;
		}
		public JSONObject toJSONObject(){
			JSONObject obj = new JSONObject();
			try{
				obj.put("type", MSG_STATE_SYNC);
				obj.put("state", state);
				obj.put("syncNonce", syncNonce);
				obj.put("opSeqNum", sequenceNum);
				obj.put("seqNumCounter", seqNumCounter);
				obj.put("lastOrderAckUUID", lastOrderAckUUID);
				obj.put("lastOpUUID", lastOpUUID);
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

	class HelloMsg extends PropMsg{
		public long seqNum;
		public HelloMsg(JSONObject msg){
			super(msg);
			seqNum = msg.optLong("seqNum");
		}
		public HelloMsg(long seqNum){
			this.seqNum = seqNum;
		}
		public JSONObject toJSONObject(){
			JSONObject obj = new JSONObject();
			try{
				obj.put("type", MSG_HELLO);
				obj.put("seqNum", seqNum);
			}catch(JSONException e){}
			return obj;
		}
	}

	class OperationOrderAckMsg extends PropMsg{
		public String uuid;
		public String msgUUID;
		public boolean predicted;
		public long sequenceNum;
		public OperationOrderAckMsg(JSONObject msg){
			super(msg);
			uuid = msg.optString("uuid");
			msgUUID = msg.optString("msgUUID");
			predicted = msg.optBoolean("predicted");
			sequenceNum = msg.optInt("seqNum");
		}
		public OperationOrderAckMsg(String msgUUID, boolean predicted, long sequenceNum){
			this.uuid = UUID.randomUUID().toString();
			this.msgUUID = msgUUID;
			this.predicted = predicted;
			this.sequenceNum = sequenceNum;
		}
		public JSONObject toJSONObject(){
			JSONObject obj = new JSONObject();
			try{
				obj.put("type", MSG_OP_ORDER_ACK);
				obj.put("uuid", uuid);
				obj.put("msgUUID", msgUUID);
				obj.put("predicted", predicted);
				obj.put("seqNum", sequenceNum);
			}catch(JSONException e){}
			return obj;
		}
	}



}

package edu.stanford.junction.simulator;

import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import edu.stanford.junction.JunctionMaker;
import edu.stanford.junction.JunctionException;
import edu.stanford.junction.api.activity.ActivityScript;
import edu.stanford.junction.api.activity.JunctionActor;
import edu.stanford.junction.api.messaging.MessageHeader;
import edu.stanford.junction.provider.xmpp.XMPPSwitchboardConfig;

/*class simThread extends Thread{
  private int NumOfMessage;
  private int NumOfParticipant;
  private int sessionID;
  simThread(int NumMsg, int NumP, int id){
  NumOfMessage = NumMsg;
  NumOfParticipant = NumP;
  sessionID = id;
  }
  public void run(){
  ActivityDescription desc = new ActivityDescription();
  //String ad = "SimSession_" + sessionID;
  //desc.setSessionID(ad);
  JSONObject platform = new JSONObject();
  try{
  platform.put("android", "http://my.realsitic.url/for_android");
  desc.addRolePlatform("simulator", "android", platform);
  } catch (Exception e) {}
  JunctionMaker maker = JunctionMaker.getInstance("prpl.stanford.edu");
  for(int actor_i = NumOfParticipant-1 ; actor_i >=0; actor_i --){
  maker.newJunction(desc, new SimActor(NumOfMessage, actor_i));
  //maker.newJunction(desc, new SimActor(NumOfMessage, 0));
  }
  }
  }*/

public class ResponseTimeSimulator {
	static int NumOfActivity = 1;
	static int NumOfMessage = 5;
	static int NumOfParticipant = 2;
	public static void main(String[] argv){
		ActivityScript desc = new ActivityScript();
		JSONObject platform = new JSONObject();
		try{
			platform.put("android", "http://my.realsitic.url/for_android");
			desc.addRolePlatform("simulator", "android", platform);
		} catch (Exception e) {}
		Date makeJuncTime = new Date(); 
		
		XMPPSwitchboardConfig config = new XMPPSwitchboardConfig("prpl.stanford.edu");
		JunctionMaker maker = JunctionMaker.getInstance(config);
		for(int actor_i = NumOfParticipant-1 ; actor_i >=0; actor_i --){
			try{
				maker.newJunction(desc, new SimActorRT(makeJuncTime.getTime(), actor_i));
			}
			catch(JunctionException e){
				e.printStackTrace(System.err);
			}
		}
		
		while(true) {
			try {
				Thread.sleep(500000);
			} catch (Exception e) {}
		}
	}
}

class SimActorRT extends JunctionActor{
	private int ID;
	private String JunctionTimestamp;
	public SimActorRT(long _JunctionTimestamp, int _ID) {
		super("SimActorRT"+_ID);
		ID = _ID; 
		JunctionTimestamp = Long.valueOf(_JunctionTimestamp).toString();
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
				simMsg.put("JunctionTimestamp", JunctionTimestamp.toString());
				Date makeMsgTime = new Date(); 
				simMsg.put("MessageTimestamp", Long.valueOf(makeMsgTime.getTime()).toString());

			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			//this.getJunction().sendMessageToRole("SimActorRT1", simMsg);
			this.getJunction().sendMessageToSession(simMsg);
		}
	}
	
	public  void onMessageReceived(MessageHeader arg0, JSONObject arg1) {
		Date recvTime = new Date(); 
		long juncResponseTime;
		long msgResponseTime;
		long juncToMsgSendingTime;
		try {
			juncResponseTime = recvTime.getTime() - Long.valueOf(arg1.get("JunctionTimestamp").toString()).longValue();
			msgResponseTime = recvTime.getTime() - Long.valueOf(arg1.get("MessageTimestamp").toString()).longValue();
			juncToMsgSendingTime = Long.valueOf(arg1.get("MessageTimestamp").toString()).longValue() - Long.valueOf(arg1.get("JunctionTimestamp").toString()).longValue();
			System.out.println(ID + ": From Make Junction to Send First Message: " + juncToMsgSendingTime);
			System.out.println(ID + ": From Make Junction to Receive First Message: " + juncResponseTime);
			System.out.println(ID + ": From Send to Receive First Message: " + msgResponseTime);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
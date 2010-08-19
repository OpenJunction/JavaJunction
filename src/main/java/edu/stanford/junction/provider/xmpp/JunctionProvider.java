package edu.stanford.junction.provider.xmpp;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Date;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.RoomInfo;
import org.json.JSONException;
import org.json.JSONObject;

import edu.stanford.junction.Junction;
import edu.stanford.junction.JunctionMaker;
import edu.stanford.junction.JunctionException;
import edu.stanford.junction.api.activity.ActivityScript;
import edu.stanford.junction.api.activity.JunctionActor;
import edu.stanford.junction.api.messaging.MessageHeader;

public class JunctionProvider extends edu.stanford.junction.provider.JunctionProvider {
	protected XMPPSwitchboardConfig mConfig;
	protected static boolean ONE_CONNECTION_PER_SESSION=false;
	
	// TODO: Can't use a single connection right now-
	// must support multiple actors in the same activity
	// and this can't be done over a single XMPP connection now.
	// BJD 2/2/10
	
	//private XMPPConnection mXMPPConnection;

	public JunctionProvider(XMPPSwitchboardConfig config) {
		mConfig = config;		
	}
	
	public Junction newJunction(URI invitation, ActivityScript script, JunctionActor actor) throws JunctionException{
		// this needs to be made more formal
		if (script == null) {
			script = new ActivityScript();
		}
		if (null == script.getHost()) {
			script.setUri(invitation);
		}
		
		XMPPConnection mXMPPConnection 
		    = getXMPPConnection(mConfig,JunctionMaker.getSessionIDFromURI(invitation));
		edu.stanford.junction.provider.xmpp.Junction jx
			= new edu.stanford.junction.provider.xmpp.Junction(script,mXMPPConnection,mConfig,this);
		
		jx.mAcceptedInvitation=invitation;
		jx.registerActor(actor);
		
		this.requestServices(jx,script);
		
		
		// creating an activity is an activity using a JunctionService.
		// Invite the JunctionMaker service to the session.
		// This service will be bundled with all Junction servers.
		//activity.requestService("JunctionMaker", mHostURL, "edu.stanford.prpl.junction.impl.JunctionMakerService");		
		return jx;
	}
	
	public ActivityScript getActivityScript(URI uri) throws JunctionException {
		
		// TODO: Move the XMPPConnection into the JunctionMaker
		// (out of Junction)
		/*
		  JunctionMaker jm = null;
		  String host = uri.getHost();
		  if (host.equals(mSwitchboard)) {
		  jm = this;
		  } else {
		  jm = new JunctionMaker(host);
		  }
		*/
		
		String host = uri.getHost();
		String sessionID = uri.getPath().substring(1);
		
		// pretty broken..
		XMPPSwitchboardConfig config = new XMPPSwitchboardConfig(host);
		XMPPConnection conn = getXMPPConnection(config,sessionID);
		
		String room = sessionID+"@" +config.getChatService();
		System.err.println("looking up info from xmpp room " + room);
		
		
		try {
			RoomInfo info = MultiUserChat.getRoomInfo(conn, room);
			String descString = info.getDescription();

			if (descString == null || descString.trim().length()==0) {
				throw new JunctionException("No MUC room description found.");
			}
			
			JSONObject descJSON = new JSONObject(descString);
			
			return new ActivityScript(descJSON);

		} catch (Exception e) {
			throw new JunctionException("Failed to initialize XMPP Chat Room.", e);
		}

	}

	private static ArrayList<XMPPConnection> sConnections = new ArrayList<XMPPConnection>();
	private static ArrayList<HashSet<String>> sConnectionSessions
		= new ArrayList<HashSet<String>>();


	class ConnectionThread extends Thread{
		public volatile XMPPConnection connection = null;
		public volatile Throwable failureReason = null;
		public volatile boolean success = false;

		private CountDownLatch waitForConnect;
		private XMPPSwitchboardConfig config;

		public ConnectionThread(XMPPSwitchboardConfig config, CountDownLatch latch){
			this.waitForConnect = latch;
			this.config = config;
		}

		public void run(){
			while(!isInterrupted()){

				if(this.connection != null){
					connection.disconnect();
				}

				connection = new XMPPConnection(config.host);

				try {
					connection.connect();
					if (config.user != null) {
						connection.login(config.user, config.password);
					} 
					else {
						connection.loginAnonymously();
					}

					if(isInterrupted()){
						success = false;
						break;
					}
					else{ // All is good. Finish up. 
						failureReason = null;
						success = true;
						waitForConnect.countDown();
						System.out.println("Got connection, ending connection thread.");
						break;
					}

				}
				catch (XMPPException e) {
					Throwable ex = e.getWrappedThrowable();
					if(ex instanceof IOException){
						failureReason = ex;
						continue;
					}
					else if(ex instanceof UnknownHostException){
						failureReason = ex;
						continue;
					}
					else {
						// Otherwise, we consider the exception
						// unrecoverable.
						failureReason = e;
						this.connection.disconnect();
						waitForConnect.countDown();
						break;
					}
				}
				catch (Exception e) {
					failureReason = e;
					this.connection.disconnect();
					waitForConnect.countDown();
					break;
				}
			}
		}
	}



	private synchronized XMPPConnection getXMPPConnection(XMPPSwitchboardConfig config, String roomid) throws JunctionException{
		final CountDownLatch waitForConnect = new CountDownLatch(1);
		ConnectionThread t = new ConnectionThread(config, waitForConnect);
		t.start();

		boolean timedOut = false;
		try{
			timedOut = !(waitForConnect.await(config.connectionTimeout, TimeUnit.MILLISECONDS));
		}
		catch(InterruptedException e){
			throw new JunctionException("Interrupted while waiting for connection.");
		}

		if(timedOut){ 
			System.out.println("Connection timed out after " + 
							   config.connectionTimeout + 
							   " milliseconds.");
			t.interrupt(); // Thread may still be looping...

			String msg = "Connection attempt failed to complete within provided timeout of " + 
				config.connectionTimeout + " milliseconds.";
			throw new JunctionException(msg, new ConnectionTimeoutException(msg));
		}

		if(t.success){
			System.out.println("Connection succeeded.");
			sConnections.add(t.connection);
			HashSet<String> set = new HashSet<String>();
			set.add(roomid);
			sConnectionSessions.add(set);
			return t.connection;
		}
		else{
			if(t.failureReason != null){
				throw new JunctionException("Connection attempt failed.", t.failureReason);
			}
			else{
				throw new JunctionException("Connection attempt failed for unknown reason.", t.failureReason);
			}
		}
	}
	
	protected synchronized void remove(edu.stanford.junction.provider.xmpp.Junction jx) {
		// O(n) can be improved, but n is small.
		XMPPConnection conn = jx.mXMPPConnection;
		for (int i=0;i<sConnections.size();i++) {
			if (sConnections.get(i).equals(conn)) {
				sConnectionSessions.get(i).remove(jx.getSessionID());
			}
		}
	}
	
	// test
	public static void main(String[] args) {
		//ConnectionConfiguration config = new ConnectionConfiguration("prpl.stanford.edu",5222);
		//XMPPConnection con = new XMPPConnection(config);
		XMPPConnection con = new XMPPConnection("prpl.stanford.edu");
	
		try {
			con.connect();
			System.out.println("Connected.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public URI generateSessionUri() {
		String session = UUID.randomUUID().toString();
		try {
			return new URI("junction://" + mConfig.host + "/" + session);
		} catch (URISyntaxException e) {
			throw new AssertionError("Invalid URI");
		}
	}

}

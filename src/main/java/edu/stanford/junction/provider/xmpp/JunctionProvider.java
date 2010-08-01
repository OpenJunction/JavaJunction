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
	
	public Junction newJunction(URI invitation, ActivityScript script, JunctionActor actor) {
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
	
	public ActivityScript getActivityScript(URI uri) {
		
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
			/*
			System.err.println("room desc " + info.getDescription());
			System.err.println("room subj " + info.getSubject());
			System.err.println("part " + info.getOccupantsCount());
			System.err.println("room " + info.getRoom());
			*/
			String descString = info.getDescription();
			if (descString == null || descString.trim().length()==0) {
				System.err.println("No MUC room description found.");
				return null;
			}
			
			JSONObject descJSON = new JSONObject(descString);
			
			return new ActivityScript(descJSON);
		} catch (XMPPException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}

	private static ArrayList<XMPPConnection> sConnections = new ArrayList<XMPPConnection>();
	private static ArrayList<HashSet<String>> sConnectionSessions
		= new ArrayList<HashSet<String>>();
	
	private synchronized XMPPConnection getXMPPConnection(XMPPSwitchboardConfig config, String roomid) {

		if (ONE_CONNECTION_PER_SESSION) {
			XMPPConnection theConnection = new XMPPConnection(config.host);
			sConnections.add(theConnection);
			HashSet<String> set = new HashSet<String>();
			set.add(roomid);
			sConnectionSessions.add(set);
			
			try {
				theConnection.connect();
				if (config.user != null) {
					theConnection.login(config.user, config.password);
				} else {
					theConnection.loginAnonymously();
				}
				
				return theConnection;
			} catch (Exception e) {
				System.err.println("Could not connect to XMPP provider");
				e.printStackTrace();
				return null;
			}
		}

		
		
		XMPPConnection theConnection = null;
		for (int i=0;i<sConnections.size();i++) {
			if (!sConnectionSessions.get(i).contains(roomid)) {
				// this connection can support this roomid.
				theConnection = sConnections.get(i);
				sConnectionSessions.get(i).add(roomid);
				
				if (!theConnection.isConnected()) {
					System.out.println("Have non-connected XMPPConnection.");
					try {
						theConnection.connect();
					} catch (XMPPException e) {
						e.printStackTrace();
					}
				}
				
				return theConnection;
			}
		}

		if (theConnection == null) {
			theConnection = new XMPPConnection(config.host);
			sConnections.add(theConnection);
			HashSet<String> set = new HashSet<String>();
			set.add(roomid);
			sConnectionSessions.add(set);
			
			try {
				theConnection.connect();
				if (config.user != null) {
					theConnection.login(config.user, config.password);
				} else {
					theConnection.loginAnonymously();
				}
				
				return theConnection;
			} catch (Exception e) {
				System.err.println("Could not connect to XMPP provider");
				e.printStackTrace();
				return null;
			}
		}
		
		return null;
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

package edu.stanford.junction.provider.xmpp;

import java.net.URI;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.RoomInfo;
import org.json.JSONException;
import org.json.JSONObject;

import edu.stanford.junction.Junction;
import edu.stanford.junction.api.activity.ActivityScript;
import edu.stanford.junction.api.activity.JunctionActor;

public class JunctionProvider extends edu.stanford.junction.provider.JunctionProvider {
	protected XMPPSwitchboardConfig mConfig;
	
	// TODO: Can't use a single connection right now-
	// must support multiple actors in the same activity
	// and this can't be done over a single XMPP connection now.
	// BJD 2/2/10
	
	//private XMPPConnection mXMPPConnection;

	public JunctionProvider(XMPPSwitchboardConfig config) {
		mConfig = config;		
	}
	
	public Junction newJunction(URI uri, JunctionActor actor) {
		ActivityScript desc = new ActivityScript();
		desc.setHost(uri.getHost());
		if (uri.getPath() != null) { // TODO: check to make sure this works for URIs w/o path
			desc.setSessionID(uri.getPath().substring(1));
		}
		
		return newJunction(desc,actor);
	}
	
	public Junction newJunction(ActivityScript desc, JunctionActor actor) {
		
		// this needs to be made more formal
		if (null == desc.getHost() && mConfig.host != null) {
			desc.setHost(mConfig.host);
		}
		
		/*
		if (mXMPPConnection == null) {
			mXMPPConnection = getXMPPConnection(mConfig);
		}
		*/
		
		// For now, every new junction gets its own XMPP connection
		// a big TODO is to fix this... BJD 2/2/10
		XMPPConnection mXMPPConnection = getXMPPConnection(mConfig);
		
		Junction jx = new edu.stanford.junction.provider.xmpp.Junction(desc,mXMPPConnection);
		jx.registerActor(actor);
		
		this.requestServices(jx,desc);
		
		
		// creating an activity is an activity using a JunctionService.
		// Invite the JunctionMaker service to the session.
		// This service will be bundled with all Junction servers.
		//activity.requestService("JunctionMaker", mHostURL, "edu.stanford.prpl.junction.impl.JunctionMakerService");		
		return jx;
	}
	
	public ActivityScript getActivityDescription(URI uri) {
		
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
		XMPPConnection conn = getXMPPConnection(config);
		
		String room = sessionID+"@conference."+host;
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

	
	private XMPPConnection getXMPPConnection(XMPPSwitchboardConfig config) {
		XMPPConnection mXMPPConnection= new XMPPConnection(config.host);
		try {
			mXMPPConnection.connect();
			if (config.user != null) {
				mXMPPConnection.login(config.user, config.password);
			} else {
				mXMPPConnection.loginAnonymously();
			}
			
			return mXMPPConnection;
		} catch (Exception e) {
			System.err.println("Could not connect to XMPP provider");
			e.printStackTrace();
		}
		return null;
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
	
}

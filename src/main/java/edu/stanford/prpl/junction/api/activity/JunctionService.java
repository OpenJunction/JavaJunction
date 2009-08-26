package edu.stanford.prpl.junction.api.activity;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.FormField;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.json.JSONObject;

import edu.stanford.prpl.junction.api.messaging.MessageHandler;
import edu.stanford.prpl.junction.api.messaging.MessageHeader;
import edu.stanford.prpl.junction.impl.Junction;
import edu.stanford.prpl.junction.impl.JunctionMaker;

public abstract class JunctionService extends JunctionActor {
	public static String SERVICE_CHANNEL="jxservice";
	
	private static Map<String,Junction>mJunctionMap;
	{
		mJunctionMap = new HashMap<String,Junction>();
	}
	
	String mRole;
	
	public abstract String getServiceName();
	
	public JunctionService() {
		super((String)null);
	}
	
	@Override
	public String[] getRoles() {
		return new String[] {mRole};
	}
	
	@Override
	public void onActivityStart() {}
	
	public final void register(String switchboard) {
		
		if (mJunctionMap.containsKey(switchboard)) return;
		
		ActivityDescription desc = new ActivityDescription();
		desc.setHost(switchboard);
		desc.setSessionID(SERVICE_CHANNEL);
		//desc.setActorID(getServiceName());
		desc.setActivityID("junction.service");
		
		Junction jx = JunctionMaker.getInstance().newJunction(desc, this);
		mJunctionMap.put(switchboard, jx);

	}
	
	public void setRole(String role) {
		mRole=role;
	}
}
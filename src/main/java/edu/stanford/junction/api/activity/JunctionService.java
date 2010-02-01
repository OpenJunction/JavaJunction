package edu.stanford.junction.api.activity;

import java.util.HashMap;
import java.util.Map;

import edu.stanford.junction.Junction;
import edu.stanford.junction.JunctionMaker;
import edu.stanford.junction.impl.xmpp.XMPPSwitchboardConfig;

// TODO: This class requires a switchboard, and is probably XMPP specific. Fix accordingly.

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
		
		ActivityScript desc = new ActivityScript();
		desc.setHost(switchboard);
		desc.setSessionID(SERVICE_CHANNEL);
		//desc.setActorID(getServiceName());
		desc.setActivityID("junction.service");
		
		//Junction jx = null; //JunctionMaker.getInstance().newJunction(desc, this);
		XMPPSwitchboardConfig config = new XMPPSwitchboardConfig(switchboard);
		JunctionMaker maker = (JunctionMaker) JunctionMaker.getInstance(config);
		Junction jx = maker.newJunction(desc, this);
		
		mJunctionMap.put(switchboard, jx);

	}
	
	public void setRole(String role) {
		mRole=role;
	}
}
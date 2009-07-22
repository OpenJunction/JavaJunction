package edu.stanford.prpl.junction.api.activity;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import edu.stanford.prpl.junction.api.messaging.MessageHandler;

public abstract class JunctionService extends JunctionActor {
	String mRole;
	
	public abstract String getServiceName();
	
	public JunctionService() {
		super(null);
	}
	
	@Override
	public String getRole() {
		return mRole;
	}
	
	@Override
	public void onActivityStart() {}
	
	public final void register(URL url) {
		
		Map<String,Object>params = new HashMap<String,Object>();
		params.put("host", url.toExternalForm());
		//mManager = new JunctionManager(params);
		
		String serviceChannel = "/srv/"+getServiceName();
		
		MessageHandler handler = getMessageHandler();
		if (handler != null) {
			//mManager.addListener(serviceChannel, getMessageHandler());
		}
	}
	
	public void setRole(String role) {
		mRole=role;
	}
	
}
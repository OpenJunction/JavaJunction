package edu.stanford.prpl.junction.api.activity;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.cometd.Client;
import org.cometd.Message;

import edu.stanford.prpl.junction.api.messaging.JunctionListener;
import edu.stanford.prpl.junction.impl.JunctionManager;
import edu.stanford.prpl.junction.impl.Junction;

public abstract class JunctionService extends JunctionActor {
	JunctionManager mManager;
	
	
	public abstract String getServiceName();
	public void onRegister() {}
	

	public final void register(URL url) {
		
		Map<String,Object>params = new HashMap<String,Object>();
		params.put("host", url.toExternalForm());
		mManager = new JunctionManager(params);
		
		String serviceChannel = "/srv/" + getServiceName();
		
		mManager.addListener(serviceChannel, new ActivityRequestListener());
	}
	
	
	
	class ActivityRequestListener implements JunctionListener {

		public void onMessageReceived(Client from, Message message) {
			// TODO: verify you got an activity request
			// somehow get the Activity object
			// that means the request needs
			// * a host
			// * an activity descriptor
			
			
			try {
				URL activityURL = new URL((String)(((Map)message.get("data")).get("activityURL")));
				Junction activity = new Junction(activityURL);
				join(activity, "JunctionManager");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}
}
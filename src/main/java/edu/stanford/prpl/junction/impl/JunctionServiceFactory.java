package edu.stanford.prpl.junction.impl;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.Map;
import org.cometd.Client;
import org.cometd.Message;

import edu.stanford.prpl.junction.api.activity.JunctionService;
import edu.stanford.prpl.junction.api.messaging.JunctionListener;
import edu.stanford.prpl.junction.api.messaging.MessageHandler;

public class JunctionServiceFactory extends JunctionService {
	
	@Override
	public MessageHandler getMessageHandler() {
		return new MessageHandler() {

			public void onMessageReceived(Client from, Message message) {
				// TODO: verify you got an activity request
				// somehow get the Activity object
				// that means the request needs
				// * a host
				// * an activity descriptor
				
				
				try {
					Map<String,Object> data = (Map<String,Object>)message.get("data");
					URL activityURL = new URL((String)((String)data.get("activityURL")));
					Junction activity = new Junction(activityURL);
					
					// TODO: support a factory mapping from serviceName => class
					String className = (String)data.get("serviceName");
					
					Class c = null;
					try {
						c = Class.forName(className);
					} catch (Exception e) {
						System.out.println("Could not find class for service " + className + ".");
					}
					
					JunctionService service = null;
					Method method = null;
					try {
						method = c.getMethod("newInstance");
					} catch (Exception e) {
						System.out.println("No newInstance method found for " + c + ".");
					}
					service = (JunctionService)method.invoke(null);
					
					String queryPart = activityURL.getQuery(); 
					String localRole = "Unknown";
					int i;
					if ((i = queryPart.indexOf("requestedRole=")) > 0) {
						localRole = queryPart.substring(i+14);
						if ((i = localRole.indexOf("&"))>0) {
							localRole = localRole.substring(0,i);
						}
					}
					
					service.setRole(localRole);
					service.join(activity);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
	}



	@Override
	public String getServiceName() {
		return "ServiceFactory";
	}

}

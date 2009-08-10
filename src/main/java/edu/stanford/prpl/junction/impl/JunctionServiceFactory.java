package edu.stanford.prpl.junction.impl;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.Map;

import org.json.JSONObject;

import edu.stanford.prpl.junction.api.activity.JunctionService;
import edu.stanford.prpl.junction.api.messaging.MessageHandler;
import edu.stanford.prpl.junction.api.messaging.MessageHeader;
import edu.stanford.prpl.junction.api.messaging.JunctionMessage;

public class JunctionServiceFactory extends JunctionService {

	@Override
	public MessageHandler getMessageHandler() {
		return new MessageHandler() {

			public void onMessageReceived(MessageHeader header, JSONObject message) {
				// TODO: verify you got an activity request
				// somehow get the Activity object
				// that means the request needs
				// * a host
				// * an activity descriptor
				
				
				try {
					
					URL activityURL = new URL((String)(message.getString("activityURL")));
					
					// TODO: support a factory mapping from serviceName => class
					String className = message.getString("serviceName");
					
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
					System.out.println("query part is " + queryPart);
					String localRole = "Unknown";
					int i;
					if ((i = queryPart.indexOf("requestedRole=")) >= 0) {
						localRole = queryPart.substring(i+14);
						if ((i = localRole.indexOf("&"))>0) {
							localRole = localRole.substring(0,i);
						}
					}
					
					System.out.println("Setting service role to " + localRole);
					service.setRole(localRole);
					
					System.out.println("service actorID is " + service.getActorID());
					JunctionMaker.getInstance().newJunction(activityURL,service);					
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

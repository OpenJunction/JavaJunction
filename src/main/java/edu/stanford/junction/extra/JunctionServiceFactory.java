package edu.stanford.junction.extra;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.util.Map;

import org.json.JSONObject;

import edu.stanford.junction.JunctionMaker;
import edu.stanford.junction.api.activity.JunctionService;
import edu.stanford.junction.api.messaging.MessageHandler;
import edu.stanford.junction.api.messaging.MessageHeader;

/**
 * TODO: This class is temporarily broken. It requires switchboard information,
 * and so is XMPP specific for now.
 * 
 * Line 71 has been commented out, resulting in nothing working here.
 *
 */
public class JunctionServiceFactory extends JunctionService {

	@Override
	public void onMessageReceived(MessageHeader header, JSONObject message) {
			// TODO: verify you got an activity request
			// somehow get the Activity object
			// that means the request needs
			// * a host
			// * an activity descriptor
			
			
			try {
				
				URI activityURI = new URI(message.getString("activity"));
				
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
				
				String queryPart = activityURI.getQuery(); 
				System.out.println("query part is " + queryPart);
				String localRole = "Unknown";
				int i;
				if ((i = queryPart.indexOf("role=")) >= 0) {
					localRole = queryPart.substring(i+14);
					if ((i = localRole.indexOf("&"))>0) {
						localRole = localRole.substring(0,i);
					}
				}
				
				System.out.println("Setting service role to " + localRole);
				service.setRole(localRole);
				
				System.out.println("service actorID is " + service.getActorID());
				//JunctionMaker.getInstance().newJunction(activityURI,service);					
			} catch (Exception e) {
				e.printStackTrace();
			}
	}



	@Override
	public String getServiceName() {
		return "ServiceFactory";
	}

	
	public static void main(String[] argv) {
		String switchboard="prpl.stanford.edu";
		
		
		JunctionService waiter = new JunctionServiceFactory();
		waiter.register(switchboard);
		while(true) {
			try {
				Thread.sleep(500000);
			} catch (Exception e) {}
		}
	}	
}
package edu.stanford.junction.director;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import edu.stanford.junction.JunctionMaker;
import edu.stanford.junction.api.activity.JunctionService;
import edu.stanford.junction.api.messaging.MessageHandler;
import edu.stanford.junction.api.messaging.MessageHeader;

/**
 * Sample message to launch a JAR:
 *  {serviceName:"test",jar:"http://prpl.stanford.edu/junction/launch/poker-dealer.jar",activity:"junction://prpl.stanford.edu/pokertest"}
 * @author Ben
 *
 */
public class JAVADirector extends JunctionService {

	@Override
	public void onMessageReceived(MessageHeader header, JSONObject message) {
			try {
				
				URI activityURI = new URI(message.getString("activity"));
				
				if (message.has("jar")) {
					URL jarURL = new URL(message.getString("jar"));
					launchJAR(jarURL,activityURI);
				} else {
					String className = message.getString("serviceName");
					launchService(activityURI,className);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

	// currently no multi-thread support
	private synchronized void launchJAR(URL jarURL, URI activityURI) {
		final String JAR_PATH = "jars/";
		
		String jarName = JAR_PATH + "/" + jarURL.getPath().substring(1).replace("/", "-");
		File jarFile = new File(jarName);
		if (!jarFile.exists()) {
			File tmpFile = new File(JAR_PATH+"/inbound.jar.tmp");
			try {
				FileOutputStream out = new FileOutputStream(tmpFile);
		        
		        InputStream in = jarURL.openStream();
		        byte[] buf = new byte[4 * 1024];
		        int bytesRead;
		        while ((bytesRead = in.read(buf)) != -1) {
		          out.write(buf, 0, bytesRead);
		        }
		        in.close();
		        out.close();

		        boolean res = tmpFile.renameTo(jarFile);
		        
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
		}
		
		if (!jarFile.exists()) {
			System.out.println("Failed to get JAR file " + jarFile.getName());
			return;
		}
		
		// Launch the new JVM
		try {
			List<String>command = new ArrayList<String>();
			command.add("java");
			command.add("-jar");
			command.add(jarFile.getAbsolutePath());
			command.add(activityURI.toString());
			
			System.out.println("Executing: " + command.toString());
			ProcessBuilder pb = new ProcessBuilder(command);
			pb.directory(jarFile.getParentFile());
			
			Process p = pb.start();
		} catch (Exception e) {
			System.out.println("failed to launch JAR.");
			e.printStackTrace();
		}
		
	}
	
	private void launchService(URI activityURI, String className) {	
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
			service = (JunctionService)method.invoke(null);
		} catch (Exception e) {
			System.out.println("No newInstance method found for " + c + ".");
		}
		
		
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
	}
	

	@Override
	public String getServiceName() {
		return "ServiceFactory";
	}

	
	public static void main(String[] argv) {
		String switchboard="prpl.stanford.edu";
		
		JunctionService waiter = new JAVADirector();
		waiter.register(switchboard);
		synchronized(waiter){
			try {
				waiter.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}	
}
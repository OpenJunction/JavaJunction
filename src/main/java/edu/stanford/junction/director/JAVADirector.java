package edu.stanford.junction.director;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
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
 * TODO: This class is temporarily broken. It requires switchboard information,
 * and so is XMPP specific for now.
 * 
 * Line 71 has been commented out, resulting in nothing working here.
 *
 */
public class JAVADirector extends JunctionService {

	@Override
	public void onMessageReceived(MessageHeader header, JSONObject message) {
			try {
				
				URI activityURI = new URI(message.getString("activity"));
				
				// TODO: support a factory mapping from serviceName => class
				String className = message.getString("serviceName");
				
				if (message.has("jar")) {
					URL jarURL = new URL(message.getString("jar"));
					launchJAR(jarURL,activityURI,className);
				} else {
					launchService(activityURI,className);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

	// currently no multi-thread support
	private synchronized void launchJAR(URL jarURL, URI activityURI, String className) {
		final String JAR_PATH = "jars/";
		
		String jarName = JAR_PATH + "/" + jarURL.getFile();
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
		        
		        tmpFile.renameTo(jarFile);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
		}
		
		if (!jarFile.exists()) {
			System.out.println("Failed to get JAR file.");
			return;
		}
		
		// Launch the new JVM
		try {
			List<String>command = new ArrayList<String>();
			command.add("java");
			command.add("-jar " + jarFile.getAbsolutePath());
			command.add(activityURI.toString());
			
			ProcessBuilder pb = new ProcessBuilder(command);
			
			Map<String, String> env = pb.environment();
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
package edu.stanford.junction.director;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.stanford.junction.JunctionMaker;
import edu.stanford.junction.api.activity.ActivityScript;
import edu.stanford.junction.api.activity.JunctionActor;
import edu.stanford.junction.api.activity.JunctionService;
import edu.stanford.junction.api.messaging.MessageHandler;
import edu.stanford.junction.api.messaging.MessageHeader;
import edu.stanford.junction.provider.xmpp.XMPPSwitchboardConfig;

/**
 * Sample message to launch a JAR:
 *  {serviceName:"test",jar:"http://prpl.stanford.edu/junction/launch/poker-dealer.jar",activity:"junction://prpl.stanford.edu/pokertest"}
 * @author Ben
 *
 */
public class JAVADirector extends JunctionActor {

	// TODO: The maker is used (poorly) to look up an activity script.
	private static JunctionMaker mMaker = null;
	private List<Activity>mActivities;

	public JAVADirector() {
		super("director");
		mActivities = new ArrayList<Activity>();
	}

	@Override
	public void onMessageReceived(MessageHeader header, JSONObject message) {
		try {
			if (message.has("action")) {
				String action = message.getString("action");
				if ("list".equals(action)) {
					JSONArray procs = new JSONArray();
					for (int i = mActivities.size()-1; i>=0; i--) {
						Activity activity = mActivities.get(i);
						try {
							activity.process.exitValue();
							// If we get this far, the process is terminated.
							mActivities.remove(i);
						} catch (Exception e) {
							// No exit value means its still running.
							JSONObject obj = new JSONObject();
							obj.put("activity", activity.uri.toString());
							procs.put(obj);
						}
					}
					JSONObject msg = new JSONObject();
					msg.put("activities",procs);
					header.getReplyTarget().sendMessage(msg);
				}

				else if ("info".equals(action)) {
					// return Junction version, platform(s), and "hints" (headless, bigscreen, etc)
					// also a nickname.

					// maybe other known directors? owner info?

					// HINTS:
					// headless ~ server
					// bigscreen ~ TV or monitor attached
					// mobile ~ phone
					// nouser ~ no direct user input (bigscreen / headless)
					// keyboard? mouse?
				}
				
				else if ("cast".equals(action)) {
					// TODO: support different platforms ("java", "web")
					String activityString = message.getString("activity");
					URI activityURI = new URI(activityString);
					
					// TODO: clean this up.
					ActivityScript script = mMaker.getActivityDescription(activityURI);
					int p = activityString.indexOf("role=");
					if (p < 0) {
						System.out.println("Invitation does not specify a role.");
						return;
					}
					
					String role = activityString.substring(p+5);
					if (role.contains("&")) {
						int q = role.indexOf("&");
						role = role.substring(0,q);
					}
					
					JSONObject spec = script.getRoleSpec(role);
					JSONObject platforms = spec.getJSONObject("platforms");
					if (platforms.has("java")) {
						JSONObject javaplat = platforms.getJSONObject("java");
						if (javaplat.has("jar")) {
							URL jarURL = new URL(javaplat.getString("jar"));
							launchJAR(jarURL,activityURI);
						} else {
							System.out.println("Warning: JAVA platform specified but no JAR found.");
						}
					} 
					
					else if (platforms.has("web")) {
						// TODO: make sure this director isn't 'headless'
						JSONObject webplat = platforms.getJSONObject("web");
						String webURL = webplat.getString("url");
						
						if (webURL.contains("?")) {
							webURL = webURL + "&";
						} else {
							webURL = webURL + "?";
						}
						webURL += "jxinvite="+URLEncoder.encode(activityString,"UTF-8");
						BrowserControl.openUrl(webURL);
					}
					
					else {
						String className = message.getString("serviceName");
						launchService(activityURI,className);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void launchJAR(URL jarURL, URI activityURI) {
		final String JAR_PATH = "jars/";

		String jarName = JAR_PATH + "/" + jarURL.getPath().substring(1).replace("/", "-");
		File jarFile = new File(jarName);
		File tmpFile = new File(jarName+".tmp");
		if (!jarFile.exists() && !tmpFile.exists()) {
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
				if (!res) {
					throw new Exception("Could not rename file.");
				}
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
			// TODO: make sure it worked
			mActivities.add(new Activity(activityURI,p));
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


	public static void main(String[] argv) {
		String switchboard="prpl.stanford.edu";
		String sessionID = "jxservice";
		
		URI activityURI = null;
		try {
			activityURI = new URI("junction://"+switchboard+"/"+sessionID);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		XMPPSwitchboardConfig config = new XMPPSwitchboardConfig(switchboard);
		mMaker = JunctionMaker.getInstance(config);
		
		JunctionActor director = new JAVADirector();
		mMaker.newJunction(activityURI, director);
		
		synchronized(director){
			try {
				director.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}	
}

class Activity {
	public URI uri;
	public Process process;

	public Activity(URI uri,Process process) {
		this.uri = uri;
		this.process = process;
	}
}

/**
 * 
 * http://javaxden.blogspot.com/2007/09/launch-web-browser-through-java.html
 *
 */
class BrowserControl{ 
	/**
	 * Method to Open the Browser with Given URL
	 * @param url
	 */
	public static Process openUrl(String url){
		String os = System.getProperty("os.name");
		Runtime runtime=Runtime.getRuntime();
		try{
			// Block for Windows Platform
			if (os.startsWith("Windows")){
				String cmd = "rundll32 url.dll,FileProtocolHandler "+ url;
				Process p = runtime.exec(cmd);
				return p;
			}
			//Block for Mac OS
			else if(os.startsWith("Mac OS")){
				Class fileMgr = Class.forName("com.apple.eio.FileManager");
				Method openURL = fileMgr.getDeclaredMethod("openURL", new Class[] {String.class});
				openURL.invoke(null, new Object[] {url});
				return null; // TODO find a better way
			}
			//Block for UNIX Platform 
			else {
				String[] browsers = {"firefox", "chrome", "opera", "konqueror", "epiphany", "mozilla", "netscape" };
				String browser = null;
				for (int count = 0; count < browsers.length && browser == null; count++)
					if (runtime.exec(new String[] {"which", browsers[count]}).waitFor() == 0)
						browser = browsers[count];
				if (browser == null)
					throw new Exception("Could not find web browser");
				else 
					return runtime.exec(new String[] {browser, url});
			}
		}catch(Exception x){
			System.err.println("Exception occurd while invoking Browser!");
			x.printStackTrace();
			return null;
		}
	}
}
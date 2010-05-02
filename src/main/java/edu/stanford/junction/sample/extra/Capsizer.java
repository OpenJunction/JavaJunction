package edu.stanford.junction.sample.extra;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import edu.stanford.junction.JunctionMaker;
import edu.stanford.junction.SwitchboardConfig;
import edu.stanford.junction.api.activity.JunctionActor;
import edu.stanford.junction.api.activity.JunctionExtra;
import edu.stanford.junction.api.messaging.MessageHeader;
import edu.stanford.junction.provider.xmpp.XMPPSwitchboardConfig;

public class Capsizer {
	
	public static void main(String[] args) {
		try {
			SwitchboardConfig switchboardConfig = new XMPPSwitchboardConfig("prpl.stanford.edu");
			JunctionMaker maker = JunctionMaker.getInstance(switchboardConfig);
			URI uri = new URI("junction://prpl.stanford.edu/capsizer");
			
			JunctionActor actor = new JunctionActor("capsizer") {
				
				@Override
				public void onMessageReceived(MessageHeader header, JSONObject message) {
					// TODO Auto-generated method stub
					System.out.println("got: " + message.toString());
				}
				
				@Override
				public List<JunctionExtra> getInitialExtras() {
					ArrayList<JunctionExtra> extras = new ArrayList<JunctionExtra>();
					extras.add(new OutboundUpperCapsizer());
					extras.add(new OutboundLowerCapsizer());
					extras.add(new LogExtra());
					return extras;
				}
				
				@Override
				public void onActivityJoin() {
					JSONObject msg = new JSONObject();
					try {
						msg.put("msg","SOOO... This Is Cool");
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					sendMessageToSession(msg);
				}
			};
			
			maker.newJunction(uri, actor);
			
			synchronized(actor){
				actor.wait();
			}
			System.out.println("Exiting.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}



class OutboundUpperCapsizer extends JunctionExtra {
	@Override
	public boolean beforeSendMessage(JSONObject msg) {
		Iterator<String> keys = msg.keys();
		
		try {
			while (keys.hasNext()) {
				String k = keys.next();
				if (msg.get(k) instanceof String) {
					String s = msg.getString(k);
					msg.remove(k);
					msg.put(k, s.toUpperCase());
				}
			}
		} catch (Exception e ) {
			e.printStackTrace();
		}
		return true;
	}
	
	@Override
	public Integer getPriority() {
		return 30;
	}
}


class OutboundLowerCapsizer extends JunctionExtra {
	@Override
	public boolean beforeSendMessage(JSONObject msg) {
		Iterator<String> keys = msg.keys();
		
		try {
			while (keys.hasNext()) {
				String k = keys.next();
				if (msg.get(k) instanceof String) {
					String s = msg.getString(k);
					msg.remove(k);
					msg.put(k, s.toLowerCase());
				}
			}
		} catch (Exception e ) {
			e.printStackTrace();
		}
		return true;
	}
	
	@Override
	public Integer getPriority() {
		return 10;
	}
}

class InboundLowerCapsizer extends JunctionExtra {
	@Override
	public boolean beforeOnMessageReceived(MessageHeader h, JSONObject msg) {
		Iterator<String> keys = msg.keys();
		try {
			while (keys.hasNext()) {
				String k = keys.next();
				if (msg.get(k) instanceof String) {
					String s = msg.getString(k);
					msg.remove(k);
					msg.put(k, s.toLowerCase());
				}
			}
		} catch (Exception e ) {
			e.printStackTrace();
		}
		return true;
	}
}

class InboundUpperCapsizer extends JunctionExtra {
	@Override
	public boolean beforeOnMessageReceived(MessageHeader h, JSONObject msg) {
		Iterator<String> keys = msg.keys();
		try {
			while (keys.hasNext()) {
				String k = keys.next();
				if (msg.get(k) instanceof String) {
					String s = msg.getString(k);
					msg.remove(k);
					msg.put(k, s.toUpperCase());
				}
			}
		} catch (Exception e ) {
			e.printStackTrace();
		}
		return true;
	}
}

class LogExtra extends JunctionExtra {
	@Override
	public void afterOnMessageReceived(MessageHeader h, JSONObject msg) {
		System.out.println("logger received " + msg.toString());
	}
}
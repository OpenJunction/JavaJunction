package edu.stanford.junction.provider.irc;

import org.junit.*;
import static org.junit.Assert.*;
import java.util.*;
import java.io.*;
import java.net.*;
import org.json.JSONObject;
import edu.stanford.junction.JunctionException;
import edu.stanford.junction.JunctionMaker;
import edu.stanford.junction.Junction;
import edu.stanford.junction.api.activity.JunctionActor;
import edu.stanford.junction.api.activity.JunctionExtra;
import edu.stanford.junction.api.activity.ActivityScript;
import edu.stanford.junction.api.messaging.MessageHeader;
import edu.stanford.junction.provider.irc.IRCSwitchboardConfig;
 
public class Test {
 
    public static void main(String args[]) {
		try{
			final JunctionActor jxActor = new JunctionActor("participant") {
					public void onActivityJoin() {
						System.out.println("Actor: Joined.");
						try{
							JSONObject msg = new JSONObject();
							msg.put("saying", "This is a tesst of a long message. The idea is that this messages length should be greater than the maximum length of an IRC message, which is 512 characters. This is a tesst of a long message. The idea is that this messages length should be greater than the maximum length of an IRC message, which is 512 characters. This is a tesst of a long message. The idea is that this messages length should be greater than the maximum length of an IRC message, which is 512 characters. This is a tesst of a long message. The idea is that this messages length should be greater than the maximum length of an IRC message, which is 512 characters. This is a tesst of a long message. The idea is that this messages length should be greater than the maximum length of an IRC message, which is 512 characters. This is a tesst of a long message. The idea is that this messages length should be greater than the maximum length of an IRC message, which is 512 characters. This is a tesst of a long message. The idea is that this messages length should be greater than the maximum length of an IRC message, which is 512 characters. This is a tesst of a long message. The idea is that this messages length should be greater than the maximum length of an IRC message, which is 512 characters. This is a tesst of a long message. The idea is that this messages length should be greater than the maximum length of an IRC message, which is 512 characters. This is a tesst of a long message. The idea is that this messages length should be greater than the maximum length of an IRC message, which is 512 characters. This is a tesst of a long message. The idea is that this messages length should be greater than the maximum length of an IRC message, which is 512 characters. This is a tesst of a long message. The idea is that this messages length should be greater than the maximum length of an IRC message, which is 512 characters. This is a tesst of a long message. The idea is that this messages length should be greater than the maximum length of an IRC message, which is 512 characters. This is a tesst of a long message. The idea is that this messages length should be greater than the maximum length of an IRC message, which is 512 characters. ");
							this.sendMessageToSession(msg);
						}
						catch(Exception e){}
					}
					public void onActivityCreate() {
						System.out.println("Actor: Activity created.");
					}
					public void onMessageReceived(MessageHeader header, JSONObject msg) {
						System.out.println("Actor: Message received - '" + msg.toString() + "'");
					}
				};
			URI url = new URI("junction://127.0.0.1:6667/jxtestsession#irc");
			IRCSwitchboardConfig config = new IRCSwitchboardConfig();
			JunctionMaker jxMaker = JunctionMaker.getInstance(config);
			Junction jx = jxMaker.newJunction(url, jxActor);
		}
		catch(JunctionException e){
			System.err.println(e.getWrappedThrowable().getMessage());
			e.printStackTrace(System.err);
			fail();
		}
		catch(Exception e){
			System.err.println(e.getMessage());
			e.printStackTrace(System.err);
			fail();
		}
	}

}
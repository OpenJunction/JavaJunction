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
			JunctionActor jxActor = new JunctionActor("participant") {
					public void onActivityJoin() {
						System.out.println("Actor: Joined.");
					}
					public void onActivityCreate() {
						System.out.println("Actor: Activity created.");
					}
					public void onMessageReceived(MessageHeader header, JSONObject msg) {
						System.out.println("Actor: Message received - '" + msg.toString() + "'");
					}
				};
			URI url = new URI("junction://chat.freenode.net:6667/jxtestsession#irc");
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
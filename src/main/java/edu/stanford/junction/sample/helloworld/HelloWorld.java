package edu.stanford.junction.sample.helloworld;

import java.net.URI;

import edu.stanford.junction.JunctionMaker;
import edu.stanford.junction.provider.xmpp.XMPPSwitchboardConfig;

public class HelloWorld {
	
	
	public static void main(String argv[]) {
		try {
			URI JX_URI = new URI("junction://prpl.stanford.edu/23917312j31hlks");
			
			Receiver receiver = new Receiver();
			Sender sender = new Sender();
			
			XMPPSwitchboardConfig config = new XMPPSwitchboardConfig("prpl.stanford.edu");
			//config.setCredentials("junction", "junction");
			JunctionMaker jm1 = JunctionMaker.getInstance(config);
			jm1.newJunction(JX_URI, receiver);
			
			JunctionMaker jm2 = JunctionMaker.getInstance(config);
			jm2.newJunction(JX_URI, sender);
			
			// keepalive sleep.
			Object sleep = new Object();
			synchronized(sleep) {
				sleep.wait();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
}
package edu.stanford.junction.sample.helloworld;

import java.net.URI;

import edu.stanford.junction.JunctionMaker;

public class HelloWorld {
	
	
	public static void main(String argv[]) {
		try {
			URI JX_URI = new URI("junction://prpl.stanford.edu/23917312j31hlks");
			
			Receiver receiver = new Receiver();
			Sender sender = new Sender();
			
			JunctionMaker jm = JunctionMaker.getInstance("prpl.stanford.edu");
			jm.newJunction(JX_URI, receiver);
			jm.newJunction(JX_URI, sender);
			
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
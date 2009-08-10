package edu.stanford.prpl.junction.sample.poker;

import java.net.URL;

import edu.stanford.prpl.junction.api.activity.JunctionService;
import edu.stanford.prpl.junction.impl.JunctionServiceFactory;

public class JunctionFactoryRunner {
	public static void main(String[] argv) {
		URL url = null;
		try {
			url = new URL("http://prpl.stanford.edu");
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		
		// register the JunctionMakerService
		// This will already be available when
		// we have a true Junction Server
		JunctionService waiter = new JunctionServiceFactory();
		waiter.register(url);
		
		while(true) {
			try {
				Thread.sleep(500000);
			} catch (Exception e) {}
		}
	}
}

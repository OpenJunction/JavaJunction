package edu.stanford.prpl.junction.sample.poker;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import edu.stanford.prpl.junction.api.activity.JunctionService;
import edu.stanford.prpl.junction.impl.Junction;
import edu.stanford.prpl.junction.impl.JunctionMaker;
import edu.stanford.prpl.junction.impl.JunctionMakerService;

public class PokerRunner {
	
	
	public static void main(String[] argv) {
		URL url = null;
		try {
			url = new URL("http://prpl.stanford.edu:8181/cometd/cometd");
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		
		// register the JunctionMakerService
		// This will already be available when
		// we have a true Junction Server
		JunctionService waiter = JunctionMakerService.newInstance();
		waiter.register(url);
		

		////////////////////////////////////////////////////
		Map<String,Object>desc = new HashMap<String, Object>();
		desc.put("ad","poker");
		
		
		JunctionMaker jm = new JunctionMaker(url);
		Junction activity = jm.newJunction(desc);
		
		new PokerDealer().join(activity);
		
		try {
			Thread.sleep(5000);
		} catch (Exception e) {}
	}
}

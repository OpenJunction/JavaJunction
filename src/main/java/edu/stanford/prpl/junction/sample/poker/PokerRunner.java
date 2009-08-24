package edu.stanford.prpl.junction.sample.poker;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import edu.stanford.prpl.junction.api.activity.ActivityDescription;
import edu.stanford.prpl.junction.api.activity.JunctionService;
import edu.stanford.prpl.junction.impl.Junction;
import edu.stanford.prpl.junction.impl.JunctionMaker;
import edu.stanford.prpl.junction.impl.JunctionMakerService;

public class PokerRunner {
	
	
	public static void main(String[] argv) {
		String url = null;
		try {
			url = "prpl.stanford.edu";
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		
		// register the JunctionMakerService
		// This will already be available when
		// we have a true Junction Server
		JunctionService waiter = JunctionMakerService.newInstance();
		//waiter.register(url);
		

		////////////////////////////////////////////////////
		ActivityDescription desc = new ActivityDescription();
		
		JunctionMaker jm = JunctionMaker.getInstance(url);
		jm.newJunction(desc, new PokerDealer());
		
		
		
		try {
			Thread.sleep(5000);
		} catch (Exception e) {}
	}
}

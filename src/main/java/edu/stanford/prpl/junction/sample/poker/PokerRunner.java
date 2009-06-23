package edu.stanford.prpl.junction.sample.poker;

import java.net.URL;

import edu.stanford.prpl.junction.api.activity.JunctionActivity;
import edu.stanford.prpl.junction.api.activity.JunctionRole;
import edu.stanford.prpl.junction.impl.JunctionMaker;

public class PokerRunner {
	
	
	public static void main(String[] argv) {
		URL url = null;
		try {
			url = new URL("http://prpl.stanford.edu:8181/cometd/cometd");
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		JunctionMaker jm = new JunctionMaker(url);
		JunctionActivity activity = jm.newActivity("poker");
		activity.registerActor("dealer", new PokerDealer());
	}
}

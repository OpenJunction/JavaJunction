package edu.stanford.prpl.junction.impl;

import edu.stanford.prpl.junction.api.activity.JunctionService;

public class JunctionMakerService extends JunctionService {
	
	@Override
	public String getServiceName() {
		return "JunctionMaker";
	}

	@Override
	public void onActivityStart() {
		System.out.println("JunctionMaker: activity has started!");
	}
	
	@Override
	public void onActivityJoin(Junction activity) {
		System.out.println("JunctionMaker joined activity w/ session id " + activity.getSessionID());
		activity.start();
	}
	
	
}

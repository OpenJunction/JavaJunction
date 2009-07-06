package edu.stanford.prpl.junction.sample.poker;

import edu.stanford.prpl.junction.api.activity.JunctionActor;
import edu.stanford.prpl.junction.impl.Junction;

public class PokerDealer extends JunctionActor {
	
	public PokerDealer() {
		super("dealer");
	}
	
	@Override
	public void onActivityStart() {
		System.out.println("Dealer: activity has been started!");
		
		dealCards();
		
	}
	
	@Override
	public void onActivityJoin(Junction activity) { // ActivityInstance, really
		super.onActivityJoin(activity);
		
		System.out.println("Dealer connected to activity.");
		//activity.start();
	}
	
	
	public void dealCards() {
		
		/*
		
		// JunctionActor extends JunctionRole
		// also has:
		// actorID, associated channels, etc.
		List<JunctionActor>roles = mActivity.getActorsForRole("player");
		for (int i = 0; i < 2; i++) {
			for (JunctionActor actor : roles) {
				
				//actor.sendObject(geSendAction(mDeck.getTopCard()));
				
				
				actor.execute("receiveCard",mDeck.getTopCard());
				
				// or generate an interface:
				// ... or if the class was written in this scope, use it:
				PokerPlayer player = (PokerPlayer)actor;
				player.receiveCard(mDeck.getTopCard());
			}
		}
		
		try {
			mActivity.waitForAck(60);
		} catch (TimeoutException e) {
			// couldn't deal all cards..
		}
		
		*/
	}
}
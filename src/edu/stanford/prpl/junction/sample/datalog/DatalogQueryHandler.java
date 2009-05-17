package edu.stanford.prpl.junction.sample.datalog;

import java.io.IOException;

import edu.stanford.prpl.junction.api.messaging.JunctionQuery;
import edu.stanford.prpl.junction.api.messaging.JunctionQueryHandler;
import edu.stanford.prpl.junction.api.object.OutboundObjectStream;

public class DatalogQueryHandler extends JunctionQueryHandler {

	@Override
	public boolean supportsQuery(JunctionQuery query) {
		return query.getQueryType().equals("Datalog");
	}
	
	
	@Override
	public void handleQuery(JunctionQuery query, OutboundObjectStream results) {
		
		// either do it here or in supportsQuery ? or somewhere else?
		// obligations:
		// if !authenticated {
		//   results = getJunctionManager().query(new ObligationsQuery("AUTHENTICATE")) // or whatever
		//   if (!authenticate(results)) { result.close(); return; }
		// }
		
		String queryText = query.getQueryText();
		System.out.println("Handling query " + queryText);
		
		int MAX_SEND=11;
		int numSent=0;
		while (numSent < MAX_SEND) {
			numSent++;
			try {
				System.out.println("going to send result #" + numSent);
				Thread.sleep(1000);
				results.send("I am result #"+numSent);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		results.close();
	  }
}

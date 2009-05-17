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
	public void handleQuery(JunctionQuery query, OutboundObjectStream result) {
		
		// either do it here or in supportsQuery ? or somewhere else?
		// obligations:
		// if !authenticated {
		//   results = getJunctionManager().query(new ObligationsQuery("AUTHENTICATE")) // or whatever
		//   if (!authenticate(results)) { result.close(); return; }
		// }
		
		String queryText = query.getQueryText();
		System.out.println("Handling query " + queryText);
		try {
			Thread.sleep(1000);
			result.send("got message: " + queryText);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// get results
		// when result comes in:
		//results.write(jsonObject.getBytes());
		
		// no more results
		//results.close();
	  }
}

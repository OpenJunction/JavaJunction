package edu.stanford.prpl.junction.api.sample.datalog;

import java.io.IOException;

import edu.stanford.prpl.junction.api.object.OutboundObjectStream;
import edu.stanford.prpl.junction.api.query.JunctionQuery;
import edu.stanford.prpl.junction.api.query.JunctionQueryHandler;

public class DatalogQueryHandler extends JunctionQueryHandler {

	@Override
	public boolean supportsQuery(JunctionQuery query) {
		return DatalogQuery.supports(query);
	}
	
	
	@Override
	public void handleQuery(JunctionQuery query, OutboundObjectStream result) {
		// query text:
		/*
		if (!(query instanceof StoredQuery)) {
			results.close(); // todo: set error message
			return;
		}
		*/
		
		// either do it here or in supportsQuery ? or somewhere else?
		// obligations:
		// if !authenticated {
		//   results = getJunctionManager().query(new ObligationsQuery("AUTHENTICATE")) // or whatever
		//   if (!authenticate(results)) { result.close(); return; }
		// }
		
		String queryText = query.getQueryText();
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

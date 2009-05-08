package edu.stanford.prpl.junction.api.sample.datalog;

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
		String queryText = query.getQueryText();
		
		// get results
		// when result comes in:
		//results.write(jsonObject.getBytes());
		
		// no more results
		//results.close();
	  }
}

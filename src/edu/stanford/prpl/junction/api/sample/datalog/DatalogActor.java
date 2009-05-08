package edu.stanford.prpl.junction.api.sample.datalog;

import java.net.URL;

import edu.stanford.prpl.junction.api.JunctionAPI;
import edu.stanford.prpl.junction.api.object.OutboundObjectStream;
import edu.stanford.prpl.junction.api.query.JunctionQuery;
import edu.stanford.prpl.junction.api.query.JunctionQueryHandler;
import edu.stanford.prpl.junction.impl.JunctionManager;
import edu.stanford.prpl.junction.impl.JunctionManagerFactory;

public class DatalogActor {
	// JunctionManager extends/implements JunctionAPI
	
	
	public static void main(String[] argv) {
		try {
			JunctionAPI jm = new JunctionManagerFactory().create(new URL("http://your.com/mine"));
		
			jm.registerQueryHandler(
				  	
			  new JunctionQueryHandler() {
  
				  public boolean supportsQuery(JunctionQuery query) {
				  	//return DatalogStoredQuery.supports(query);
					  return DatalogQuery.supports(query);
				  }
				  
				  public void handleQuery(JunctionQuery query, OutboundObjectStream results) {
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
						results.close();
				  }
			  }
			  );
		
		} catch (Exception e) {
			System.err.println("fail.");
			e.printStackTrace();
		}
	}
	
	
}

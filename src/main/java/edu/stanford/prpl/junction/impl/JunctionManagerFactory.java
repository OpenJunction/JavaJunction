package edu.stanford.prpl.junction.impl;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import edu.stanford.prpl.junction.api.JunctionAPI;
import edu.stanford.prpl.junction.api.JunctionFactory;

public class JunctionManagerFactory implements JunctionFactory {
	// TODO: I should probably be a singleton per-junction session.
	// So if the JSONObject activity is the same, return the same object.
	
	// For testing, use the same JM.
	static JunctionManager mJunctionInstance;
	static JunctionManagerFactory mFactoryInstance;
	
	
	
	public JunctionManagerFactory getInstance() {
		if (null == mFactoryInstance) {
			mFactoryInstance = new JunctionManagerFactory();
		}
		
		return mFactoryInstance;
	}
	
	public JunctionManager create(Map<String,Object> activity) {
		if (mJunctionInstance == null) {
			mJunctionInstance = new JunctionManager(activity);
		}
		
		return mJunctionInstance;
	}

	public JunctionManager create(URL url) {
		if (mJunctionInstance == null) {
			Map<String,Object> desc = new HashMap<String,Object>();
			try {
				desc.put("host",url.toExternalForm());
			} catch (Exception e) {
				throw new IllegalArgumentException("Malformed host URL");
			}
			mJunctionInstance = new JunctionManager(desc);
		}
		
		return mJunctionInstance;
	}

}

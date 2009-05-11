package edu.stanford.prpl.junction.api.sample.datalog;

import java.util.Map;

import edu.stanford.prpl.junction.api.messaging.JunctionQuery;

public class DatalogQuery implements JunctionQuery {
	protected  final static String TYPE = "Datalog";
	
	
	private String mQuery;
	private boolean mPersist=false;
	
	public DatalogQuery(String query) {
		mQuery=query;
	}
	
	public String getQueryText() {
		return mQuery;
	}

	public String getType() {
		// TODO Auto-generated method stub
		return TYPE;
	}
	
	public static boolean supports(JunctionQuery query) {
		return true;
	}

	public Map<String, Object> getParameterMap() {
		return null;
	}
	
	public boolean isPersistent() {
		return mPersist;
	}
	
	public void persist(boolean shouldI) {
		mPersist=shouldI;
	}

}

package edu.stanford.prpl.junction.api.sample.datalog;

import java.util.Map;

import edu.stanford.prpl.junction.api.query.JunctionQuery;

public class DatalogQuery implements JunctionQuery {
	protected  final String mType = "DATALOG";
	
	public String getQueryText() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getType() {
		// TODO Auto-generated method stub
		return mType;
	}
	
	public static boolean supports(JunctionQuery query) {
		return true;
	}

	public Map<String, Object> getParameterMap() {
		return null;
	}

}

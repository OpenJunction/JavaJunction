package edu.stanford.junction.props;

import org.json.JSONObject;

public interface IPropState extends IStringifiable{
	IPropState applyOperation(IPropStateOperation operation);
	String hash();
	IPropState copy();
}

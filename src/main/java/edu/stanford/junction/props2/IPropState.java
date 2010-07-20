package edu.stanford.junction.props2;

import org.json.JSONObject;

public interface IPropState{
	IPropState applyOperation(JSONObject operation);
	JSONObject toJSON();
	IPropState copy();
}

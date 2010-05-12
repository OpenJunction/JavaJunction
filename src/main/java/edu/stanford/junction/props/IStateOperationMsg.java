package edu.stanford.junction.props;

import org.json.JSONObject;

public interface IStateOperationMsg extends IPropMsg{
	boolean isPredicted();
	String getUUID();
	long getFinStateNum();
	long getSequenceNum();
	void setSequenceNum(long num);
	long getPredictedStateNum();
	HistoryMAC getHistoryMAC();
	IPropStateOperation getOp();
	IStateOperationMsg newWithOp(IPropStateOperation op);
}
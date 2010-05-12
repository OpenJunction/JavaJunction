package edu.stanford.junction.props;

import org.json.JSONObject;

public interface IStateOperationMsg extends IPropMsg{
	boolean isPredicted();
	String getUUID();
	long getSequenceNum();
	void setSequenceNum(long num);
	long getPredictedSeqNum();
	HistoryMAC getHistoryMAC();
	IPropStateOperation getOp();
	IStateOperationMsg newWithOp(IPropStateOperation op);
}
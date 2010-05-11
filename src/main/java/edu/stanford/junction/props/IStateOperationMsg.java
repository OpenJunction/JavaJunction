package edu.stanford.junction.props;

import org.json.JSONObject;

public interface IStateOperationMsg extends IPropMsg{
	String getUUID();
	long getAckStateNumber();
	long getPredictedStateNumber();
	HistoryMAC getHistoryMAC();
	IPropStateOperation getOp();
}
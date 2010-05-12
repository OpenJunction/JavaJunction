package edu.stanford.junction.props;
import org.json.JSONObject;

public interface IPropMsg {
	JSONObject toJSONObject();
	String getSenderReplicaUUID();
	String getSenderReplicaName();
	int getType();
	String getSenderActor();
}
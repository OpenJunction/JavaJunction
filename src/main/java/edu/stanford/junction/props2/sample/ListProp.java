package edu.stanford.junction.props2.sample;
import org.json.JSONObject;
import java.util.*;
import edu.stanford.junction.props2.IPropState;

public class ListProp extends CollectionProp {

	public ListProp(String propName, String propReplicaName, IPropState s, long seqNum){
		super(propName, propReplicaName, s, seqNum);
	}

	public ListProp(String propName, String propReplicaName, IPropState s){
		this(propName, propReplicaName, s, 0);
	}

	public ListProp(String propName, String propReplicaName){
		this(propName, propReplicaName, new ListState());
	}

}



package edu.stanford.junction.props2.sample;
import org.json.JSONObject;
import java.util.*;

public class SetProp extends CollectionProp {
	public SetProp(String propName, String propReplicaName, Collection<JSONObject> items){
		super(propName, propReplicaName, new SetState(items));
	}
	public SetProp(String propName, String propReplicaName){
		this(propName, propReplicaName, new ArrayList<JSONObject>());
	}
}


class SetState extends CollectionState{

	public SetState(Collection<JSONObject> inItems){
		super(inItems);
	}

	public SetState(){
		this(new ArrayList<JSONObject>());
	}

	public Collection<JSONObject> items(){
		return Collections.unmodifiableSet((Set)items);
	}

	protected CollectionState newWith(Collection<JSONObject> items){
		return new SetState(items);
	}

	protected Collection<JSONObject> newImplCollection(){
		return new HashSet<JSONObject>();
	}

}




package edu.stanford.junction.props2.sample;
import org.json.JSONObject;
import java.util.*;

public class ListProp extends CollectionProp {
	public ListProp(String propName, String propReplicaName, Collection<JSONObject> items){
		super(propName, propReplicaName, new ListState(items));
	}
	public ListProp(String propName, String propReplicaName){
		this(propName, propReplicaName, new ArrayList<JSONObject>());
	}
}


class ListState extends CollectionState{

	public ListState(Collection<JSONObject> inItems){
		super(inItems);
	}

	public ListState(){
		this(new ArrayList<JSONObject>());
	}

	public Collection<JSONObject> items(){
		return Collections.unmodifiableList((List)items);
	}

	protected CollectionState newWith(Collection<JSONObject> items){
		return new ListState(items);
	}

	protected Collection<JSONObject> newImplCollection(){
		return new ArrayList<JSONObject>();
	}

}




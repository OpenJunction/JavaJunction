package edu.stanford.junction.props2.sample;
import org.json.JSONObject;
import java.util.*;
import edu.stanford.junction.props2.IPropState;


public class ListState extends CollectionState{

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




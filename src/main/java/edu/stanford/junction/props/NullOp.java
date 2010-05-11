package edu.stanford.junction.props;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.Random;


public class NullOp implements IPropStateOperation{

	public String stringify(){
		JSONObject obj = new JSONObject();
		try{
			obj.put("type", "null");
		}catch(JSONException e){}
		return obj.toString();
	}

}

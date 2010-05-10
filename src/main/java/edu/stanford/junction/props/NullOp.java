package edu.stanford.junction.props;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.Random;


public class NullOp implements IPropStateOperation{
	private long nonce;

	public NullOp(long nonce){
		this.nonce = nonce;
	}

	public NullOp(){
		this((new Random()).nextLong());
	}

	public long getNonce(){ 
		return this.nonce; 
	}

	private JSONObject toJSONObject(){
		JSONObject obj = new JSONObject();
		try{
			obj.put("type", "null");
			obj.put("nonce", nonce);
		}catch(JSONException e){}
		return obj;
	}

	public String stringify(){
		return toJSONObject().toString();
	}
}

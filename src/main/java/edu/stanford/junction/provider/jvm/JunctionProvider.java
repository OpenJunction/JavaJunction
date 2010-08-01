package edu.stanford.junction.provider.jvm;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import edu.stanford.junction.Junction;
import edu.stanford.junction.api.activity.ActivityScript;
import edu.stanford.junction.api.activity.JunctionActor;

public class JunctionProvider extends edu.stanford.junction.provider.JunctionProvider {
	
	public JunctionProvider(JVMSwitchboardConfig config) {
		
	}
	
	@Override
	public ActivityScript getActivityScript(URI uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Junction newJunction(URI uri, ActivityScript script, JunctionActor actor) {
		return new edu.stanford.junction.provider.jvm.Junction(uri,null,actor);
	}

	@Override
	public URI generateSessionUri() {
		try {
			String session = UUID.randomUUID().toString();
			return new URI("junction://" + session + "#jvm");
		} catch (URISyntaxException e) {
			throw new AssertionError("Invalid URI");
		}
	}
}

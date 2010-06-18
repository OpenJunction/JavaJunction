package edu.stanford.junction.provider.jvm;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import edu.stanford.junction.Junction;
import edu.stanford.junction.api.activity.ActivityScript;
import edu.stanford.junction.api.activity.JunctionActor;

public class JunctionProvider extends edu.stanford.junction.provider.JunctionProvider {
	
	@Override
	public ActivityScript getActivityScript(URI uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Junction newJunction(URI uri, JunctionActor actor) {
		return new edu.stanford.junction.provider.jvm.Junction(uri,null,actor);
	}

	@Override
	public Junction newJunction(ActivityScript desc, JunctionActor actor) {
		// TODO: The way sessionIDs are created and managed is garbage.
		// fix.
		
		String uuid = UUID.randomUUID().toString();
		URI uri = null;
		try {
			uri = new URI("junction://"+uuid+"#jvm");
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
		return new edu.stanford.junction.provider.jvm.Junction(uri,desc,actor);
	}

	public JunctionProvider(JVMSwitchboardConfig config) {
		
	}
}

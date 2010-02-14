package edu.stanford.junction.provider;

import java.net.URI;

import edu.stanford.junction.Junction;
import edu.stanford.junction.api.activity.ActivityScript;
import edu.stanford.junction.api.activity.JunctionActor;

public interface JunctionProvider {
	public abstract Junction newJunction(URI uri, JunctionActor actor);
	public abstract Junction newJunction(ActivityScript desc, JunctionActor actor);
	public abstract ActivityScript getActivityDescription(URI uri);
}

package edu.stanford.junction.props2;
import org.json.JSONObject;

public interface IProp{

	/**
	 * The internal counter that tracks how many operations 
	 * have been executed on this prop's state. For a given state,
	 * this number should be the same at all peers.
	 */
	public long getSequenceNum();

	/**
	 * The name of the prop at large. All peers must share this name.
	 */
	public String getPropName();

	public String getPropReplicaName();

	public void addChangeListener(IPropChangeListener listener);

	public void removeChangeListener(IPropChangeListener listener);

	public void removeChangeListenersOfType(String type);

	public void removeAllChangeListeners();

	public void addOperation(JSONObject operation);

	public IProp newFresh();

}
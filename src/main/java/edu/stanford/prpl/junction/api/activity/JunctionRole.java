package edu.stanford.prpl.junction.api.activity;

public abstract class JunctionRole {
	protected JunctionActivity mActivity;
	
	public void onActivityJoin(JunctionActivity activity) {
		mActivity=activity;
	}
	
	public abstract void onActivityStart(); 
}

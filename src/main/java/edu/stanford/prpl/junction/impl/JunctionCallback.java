package edu.stanford.prpl.junction.impl;

import java.io.IOException;

import edu.stanford.prpl.junction.api.object.InboundObjectStream;


public abstract class JunctionCallback implements 
		edu.stanford.prpl.junction.api.JunctionCallback {
	
	private boolean isBound=false;
	private boolean remoteClose=true;
	private InboundObjectStream mStream;
	private Thread mThread;
	
	protected void bind(InboundObjectStream stream) throws IOException {
		if (isBound) {
			throw new IOException("bind() called on an already bound callback object.");
		}
		
		isBound=true;
		mStream=stream;
		
		mThread = new Thread() {
			@Override
			public void run() {
				while (mStream.waitForObject()) {
					onObjectReceived(mStream);
				}
				
				onTermination(remoteClose); // trigger termination handler
									 // true == caused by remote party.
			}
		};
		mThread.start();
	}
	
	public abstract void onObjectReceived(InboundObjectStream stream);
	public void onTermination(boolean wasRemote) {}
	
	
	public void terminate() {
		remoteClose=false;
		if (mStream != null) {
			mStream.close();
		}
	}
}

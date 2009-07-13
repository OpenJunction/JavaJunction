package edu.stanford.prpl.junction.impl.object;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import org.cometd.Client;
import org.cometd.Message;

import edu.stanford.prpl.junction.api.messaging.JunctionEndOfStream;
import edu.stanford.prpl.junction.api.messaging.JunctionListener;
import edu.stanford.prpl.junction.api.object.InboundObjectStream;
import edu.stanford.prpl.junction.impl.JunctionManager;

public class BayeuxInboundObjectStream implements InboundObjectStream {
	String mChannel;
	Queue<Object>mQueue;
	JunctionListener mListener;
	boolean isClosed = false;
	
	public BayeuxInboundObjectStream(JunctionManager jm, String channel) {
		mChannel = channel;
		mQueue = new LinkedList<Object>(); // handle synchronization internally
		
		mListener = new JunctionListener() {
			public void onMessageReceived(Client from, Message message) {
				
				if (message == null) return;
				synchronized(mQueue) {
					mQueue.add(message); // should add message here, but pooling is breaking this
					mQueue.notify();
				}
			}
		};
		
		jm.addListener(channel, mListener);
	}
	
	public void close() {
		synchronized(mQueue){
			mQueue.clear();
			isClosed = true;
			mQueue.notify();
			
			// todo: send notice to connected OutboundObjectStream.
		}
	}

	public boolean hasObject() {
		return (mQueue.size() > 0 && !(mQueue.peek() instanceof JunctionEndOfStream));
	}

	public Object receive() throws IOException {
		synchronized (mQueue) {
			if (!waitForObject()) {
				return null;
			}
			
			Object obj = mQueue.remove();
			if (obj instanceof JunctionEndOfStream) {
				this.close();
				return null;
			}
			
			return obj;
		}
		
	}

	public boolean waitForObject() {
		synchronized(mQueue) {
			while (!isClosed && mQueue.size() == 0) {
				try {
					mQueue.wait();
				} catch (InterruptedException e) {
					
				}
			}
		}
		
		return (mQueue.size() > 0 && !(mQueue.peek() instanceof JunctionEndOfStream));
	}

}
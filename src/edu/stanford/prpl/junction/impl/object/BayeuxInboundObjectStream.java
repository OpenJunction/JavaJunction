package edu.stanford.prpl.junction.impl.object;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import org.cometd.Client;
import org.cometd.Message;

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
				if (message.getData() == null) return;
				synchronized(mQueue) {
					System.out.println("stream got a message: " + message);
					mQueue.add(message);
					mQueue.notify();
				}
			}
		};
		
		System.out.println("binding stream to channel " + channel);
		jm.addListener(channel, mListener);
	}
	
	public void close() {
		mQueue.clear();
		mQueue = null;
		isClosed = true;
	}

	public boolean hasObject() {
		return (mQueue.size() > 0);
	}

	public Object receive() throws IOException {
		if (!waitForObject()) {
			return null;
		}
		
		return mQueue.remove();
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
		
		return (mQueue.size() > 0);
	}

}

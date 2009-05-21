package edu.stanford.prpl.junction.impl.object;

import java.io.IOException;
import java.util.List;

import edu.stanford.prpl.junction.api.object.OutboundObjectStream;
import edu.stanford.prpl.junction.impl.JunctionManager;

public class BayeuxOutboundObjectStream implements OutboundObjectStream {
	private String mChannel;
	private JunctionManager mJunctionManager;
	
	public BayeuxOutboundObjectStream(JunctionManager jm, String channel) {
		mChannel=channel;
		mJunctionManager=jm;
	}
	
	public void close() {
		// TODO Auto-generated method stub

	}

	public void flush() {
		// TODO Auto-generated method stub

	}

	public void send(Object outbound) throws IOException {
		mJunctionManager.publish(mChannel, outbound);
	}

	public void sendList(List<Object> outboundList) throws IOException {
		if (outboundList == null) {
		     return;
		}
		for (Object o : outboundList) {
		    mJunctionManager.publish(mChannel, o);
		}

	}

}

package edu.stanford.prpl.junction.sample.datalog;

import java.io.IOException;

import edu.stanford.prpl.junction.api.object.InboundObjectStream;
import edu.stanford.prpl.junction.impl.JunctionCallback;

public class DatalogCallback extends JunctionCallback {

	public void onObjectReceived(InboundObjectStream stream) {
		try {
			Object obj;
			while (stream.waitForObject()) {
				obj = stream.receive();
				System.out.println("Callback got result: " + obj.toString());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}

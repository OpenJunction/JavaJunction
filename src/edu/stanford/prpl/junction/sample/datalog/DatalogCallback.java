package edu.stanford.prpl.junction.sample.datalog;

import java.io.IOException;

import edu.stanford.prpl.junction.api.JunctionCallback;
import edu.stanford.prpl.junction.api.object.InboundObjectStream;

public class DatalogCallback extends JunctionCallback {

	public void onMessageReceived(InboundObjectStream stream) {
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

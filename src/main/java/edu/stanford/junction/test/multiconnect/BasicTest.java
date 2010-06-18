package edu.stanford.junction.test.multiconnect;

import java.net.URI;

import org.junit.Test;

import edu.stanford.junction.JunctionMaker;
import edu.stanford.junction.provider.jvm.JVMSwitchboardConfig;
import edu.stanford.junction.provider.xmpp.XMPPSwitchboardConfig;

public class BasicTest {
	
	public static void main(String[] args) {
		BasicTest t = new BasicTest();
		t.multiconnectTest();
	}
	
	@Test
	public void multiconnectTest() {
		try {
			URI JX_URI1 = new URI("junction://prpl.stanford.edu/23917312j31hlks");
			URI JX_URI2 = new URI("junction://prpl.stanford.edu/jhfhfn");
			
			Receiver receiver1 = new Receiver("R1");
			Sender sender1 = new Sender("S1");
			
			Receiver receiver2 = new Receiver("R2");
			Sender sender2 = new Sender("S2");
			
			//XMPPSwitchboardConfig config = new XMPPSwitchboardConfig("prpl.stanford.edu");
			JVMSwitchboardConfig config = new JVMSwitchboardConfig();
			JunctionMaker jm1 = JunctionMaker.getInstance(config);
			
			System.out.println("creating R1");
			jm1.newJunction(JX_URI1, receiver1);
			
			System.out.println("creating S1");
			//JunctionMaker jm2 = JunctionMaker.getInstance(config);
			jm1.newJunction(JX_URI1, sender1);
			
			
			System.out.println("creating R2");
			jm1.newJunction(JX_URI2, receiver2);
			System.out.println("creating S2");
			jm1.newJunction(JX_URI2, sender2);
			
			// keepalive sleep.
			Object sleep = new Object();
			synchronized(sleep) {
				sleep.wait();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
}
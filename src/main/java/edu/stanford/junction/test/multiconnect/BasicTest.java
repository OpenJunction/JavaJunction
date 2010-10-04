/*
 * Copyright (C) 2010 Stanford University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


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
			
			XMPPSwitchboardConfig config = new XMPPSwitchboardConfig("prpl.stanford.edu");
			//JVMSwitchboardConfig config = new JVMSwitchboardConfig();
			JunctionMaker jm1 = JunctionMaker.getInstance(config);
			
			System.out.println("binding R1");
			jm1.newJunction(JX_URI1, receiver1);
			
			System.out.println("binding S1");
			//JunctionMaker jm2 = JunctionMaker.getInstance(config);
			jm1.newJunction(JX_URI1, sender1);
			
			
			System.out.println("binding R2");
			jm1.newJunction(JX_URI2, receiver2);
			System.out.println("binding S2");
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
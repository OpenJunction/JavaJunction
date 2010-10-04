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


package edu.stanford.junction.sample.helloworld;

import java.net.URI;

import edu.stanford.junction.JunctionMaker;
import edu.stanford.junction.provider.xmpp.XMPPSwitchboardConfig;

public class HelloWorld {
	
	
	public static void main(String argv[]) {
		try {
			URI JX_URI = new URI("junction://prpl.stanford.edu/23917312j31hlks");
			
			Receiver receiver = new Receiver();
			Sender sender = new Sender();
			
			XMPPSwitchboardConfig config = new XMPPSwitchboardConfig("prpl.stanford.edu");
			//config.setCredentials("junction", "junction");
			JunctionMaker jm1 = JunctionMaker.getInstance(config);
			jm1.newJunction(JX_URI, receiver);
			
			JunctionMaker jm2 = JunctionMaker.getInstance(config);
			jm2.newJunction(JX_URI, sender);
			
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
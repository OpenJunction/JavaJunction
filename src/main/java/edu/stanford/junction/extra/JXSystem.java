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


package edu.stanford.junction.extra;

import org.json.JSONObject;

import edu.stanford.junction.api.activity.JunctionExtra;
import edu.stanford.junction.api.messaging.MessageHeader;

/**
 * Handles internal System requests. This may include handover to OOB-transport
 * (or, at least, serves as an example for how to do OOB transport)
 * @author bjdodson
 *
 */
public class JXSystem extends JunctionExtra {	
		protected static final String JX_SYSTEM_NS = "JXSYSTEMMSG";
		
		public JXSystem() {
		
		}

		@Override
		public boolean beforeSendMessage(JSONObject msg) {
			// TODO: if message has JXSYSTEMMSG flag, don't allow.
			return super.beforeSendMessage(msg);
		}
		
		@Override
		public boolean beforeOnMessageReceived(MessageHeader h, JSONObject msg) {
			if (msg.has(JX_SYSTEM_NS)) {
				// do something
				return false;
			}
			
			return true;	
		}
		
		// TODO: in case of out-of-band transport, some external
		// process triggers a call to this.getActor().onMessageReceived(h,msg);
		// make sure that runs through the appropriate Extras.
		
		public static void main(String ... args) {
			new JXSystem().test();
		}
		
		// public void test() {} // overrides testing.  geucy.
}

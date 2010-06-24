package edu.stanford.junction.sample.extra;

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

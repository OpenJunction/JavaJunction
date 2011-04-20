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

import edu.stanford.junction.api.activity.JunctionActor;
import edu.stanford.junction.api.activity.JunctionExtra;
import edu.stanford.junction.api.messaging.MessageHeader;

/**
 * A pulse. Pulses are queued for transmission at a duration approximated by mSleep.
 * @author bjdodson
 *
 */
public class Pulse extends JunctionExtra {
		private long mSleep=-1;
		private static final int DEFAULT_SLEEP=1000;
		Heartbeat mHeartbeat;
	
		public Pulse(long t){
			mSleep=t;
		}
		
		public Pulse() {
			mSleep=DEFAULT_SLEEP;
		}
		
		/**
		 * TODO: Comments show up and act as
		 * AUTOMATIC DOCUMENTATION. Use rich HTML;
		 * can just send links, but editor can render inline (or, at least, in dedicated frame)
		 */
		@Override
		public void afterActivityJoin() {
			mHeartbeat = new Heartbeat(mSleep);
		}
		
		class Heartbeat {
			private JSONObject helpimalive = new JSONObject();
			private long mTics=0;
			
			Heartbeat(final long time) {
				new Thread(){
					@Override
					public void run() {
						try {
							while (true/*Thread.sleep(time)*/) {
								try {
									helpimalive.put("tic",mTics++); // TODO: check for dirty deletion
									getActor().sendMessageToSession(helpimalive);
								} catch (Exception e) {
									//getActor().getLogger().log();
									// also hijack system.out (out.jacking)
									e.printStackTrace();
								}
								Thread.sleep(time);
							}
						} catch (Exception e) {
							
						}
					}
				}.start();
			}
		}
		
		public static void main(String ... args) {
			new Pulse().test();
		}
		
		// public void test() {} // overrides testing.  geucy.
}

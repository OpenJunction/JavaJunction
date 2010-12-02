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


package edu.stanford.junction.provider.irc;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MessageDemultiplexer  {

	public static class CompleteMessage{
		public final String from;
		public final String msg;
		public CompleteMessage(String from, String msg){
			this.from = from;
			this.msg = msg;
		}
	}

	private Map<String,String> buffer = new HashMap<String,String>();
	private List<CompleteMessage> complete = new ArrayList<CompleteMessage>();

	public void addFragment(String frag, String from){
		String cur = "";
		if(buffer.containsKey(from)){
			cur = buffer.get(from);
		}
		if(frag.startsWith(MessageMultiplexer.TERM)){
			String lastFrag = frag.substring(MessageMultiplexer.TERM.length());
			complete.add(new CompleteMessage(from, cur + lastFrag));
			buffer.remove(from);
		}
		else{
			buffer.put(from, cur + frag);
		}
	}

	public List<CompleteMessage> drainCompleteMessages(){
		List<CompleteMessage> result = new ArrayList<CompleteMessage>(complete);
		complete.clear();
		return result;
	}

}
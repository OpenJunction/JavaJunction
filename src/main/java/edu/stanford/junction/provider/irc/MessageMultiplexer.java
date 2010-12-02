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

public class MessageMultiplexer  {

	public static final int FRAGMENT_SIZE = 400; // Should be safe for IRC
	public static final String TERM = "^^^"; 

	public List<String> divide(String msg){
		ArrayList<String> fragments = new ArrayList<String>();
		String str = msg;
		while(str.length() > FRAGMENT_SIZE){
			String frag = str.substring(0,FRAGMENT_SIZE);
			fragments.add(frag);
			str = str.substring(FRAGMENT_SIZE);
		}
		String lastFrag = TERM + str;
		fragments.add(lastFrag);
		return fragments;
	}

}
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


package edu.stanford.junction.api.activity;

import java.net.URI;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * A listing of roles that should be cast in an activity
 * and an associated director that can fill the
 * casting request.
 * 
 * @author Ben Dodson
 *
 */
public class Cast {
		List<String> mRoles;
		List<URI> mDirectors;
		
		public Cast() {
			mRoles = new LinkedList<String>();
			mDirectors = new LinkedList<URI>();
		}
		
		public Cast(List<String>roles,List<URI>directors) {
			mRoles = roles;
			mDirectors = directors;
		}
		
		public void add(String role, URI castingDirector) {
			mRoles.add(role);
			mDirectors.add(castingDirector);
		}
		
		public void remove(int i) {
			mRoles.remove(i);
			mDirectors.remove(i);
		}
		
		public String getRole(int i) { return mRoles.get(i); }
		public URI getDirector(int i) { return mDirectors.get(i); }
		public int size() { return mRoles.size(); }
	}
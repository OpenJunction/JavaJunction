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


package edu.stanford.junction.props2;
import org.json.JSONObject;

public interface IProp{

	/**
	 * The internal counter that tracks how many operations 
	 * have been executed on this prop's state. For a given state,
	 * this number should be the same at all peers.
	 */
	public long getSequenceNum();

	/**
	 * The name of the prop at large. All peers must share this name.
	 */
	public String getPropName();

	public String getPropReplicaName();

	public void addChangeListener(IPropChangeListener listener);

	public void removeChangeListener(IPropChangeListener listener);

	public void removeChangeListenersOfType(String type);

	public void removeAllChangeListeners();

	public void addOperation(JSONObject operation);

	public IProp newFresh();

}
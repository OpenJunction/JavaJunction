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


package edu.stanford.junction.props;

public class UnexpectedOpPairException extends Exception{

	public IPropStateOperation o1;
	public IPropStateOperation o2;

	public UnexpectedOpPairException(IPropStateOperation o1, IPropStateOperation o2){
		super();
		this.o1 = o1;
		this.o2 = o2;
	}

	public String toString(){
		return "Unexpected pairing of operation in transposeForward: " + o1 + ", " + o2 + ".";
	}
}
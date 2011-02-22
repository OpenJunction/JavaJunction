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
import java.util.*;
import java.io.*;


public class PropStats{

	public Long startTime = System.nanoTime();

	public ArrayList<Long> messageRTTTimes = new ArrayList<Long>();
	public ArrayList<Long> messageRTTs = new ArrayList<Long>();

	public ArrayList<Long> predictionQLengthTimes = new ArrayList<Long>();
	public ArrayList<Integer> predictionQLengths = new ArrayList<Integer>();

	public ArrayList<Long> conflictTimes = new ArrayList<Long>();
	public ArrayList<Integer> conflictLengths = new ArrayList<Integer>();


	public void addMessageRTT(long when, long elapsed){
		messageRTTTimes.add(when - startTime);
		messageRTTs.add(elapsed);
	}

	public void addPredictionQLength(long when, int len){
		predictionQLengthTimes.add(when - startTime);
		predictionQLengths.add(len);
	}

	public void addConflict(long when, int len){
		conflictTimes.add(when - startTime);
		conflictLengths.add(len);
	}

	public void writeRTT(Writer out){
		try{
			for(int i = 0; i < messageRTTs.size(); i++){
				Long rtt = messageRTTs.get(i);
				Long t = messageRTTTimes.get(i);
				out.write(t + "\t" + rtt + "\n");
			}
		}
		catch(IOException e){
			e.printStackTrace(System.err);
		}
	}

	public void writePredictions(Writer out){
		try{
			for(int i = 0; i < predictionQLengths.size(); i++){
				Integer len = predictionQLengths.get(i);
				Long t = predictionQLengthTimes.get(i);
				out.write(t + "\t" + len + "\n");
			}
		}
		catch(IOException e){
			e.printStackTrace(System.err);
		}
	}

	public void writeConflicts(Writer out){
		try{
			for(int i = 0; i < conflictLengths.size(); i++){
				Integer len = conflictLengths.get(i);
				Long t = conflictTimes.get(i);
				out.write(t + "\t" + len + "\n");
			}
		}
		catch(IOException e){
			e.printStackTrace(System.err);
		}
	}


}

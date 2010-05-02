package edu.stanford.junction.provider;

import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;

import org.json.JSONObject;

import edu.stanford.junction.api.activity.JunctionExtra;
import edu.stanford.junction.api.messaging.MessageHeader;

public class ExtrasDirector {
	
	Comparator<JunctionExtra>mComparator = new Comparator<JunctionExtra>() {
		@Override
		public int compare(JunctionExtra o1, JunctionExtra o2) {
			return o1.getPriority().compareTo(o2.getPriority());
		}
	};
	TreeSet<JunctionExtra>mExtras = new TreeSet<JunctionExtra>(mComparator);
	
	/**
	 * Returns true if onMessageReceived should be called in the usual way.
	 * @param header
	 * @param message
	 * @return
	 */
	public boolean beforeOnMessageReceived(MessageHeader header, JSONObject message) {
		Iterator<JunctionExtra>iter = mExtras.descendingIterator();
		while (iter.hasNext()) {
			JunctionExtra ex = iter.next();
			if (!ex.beforeOnMessageReceived(header, message))
				return false;
		}
		return true;
	}
	
	public void afterOnMessageReceived(MessageHeader header, JSONObject message) {
		Iterator<JunctionExtra>iter = mExtras.descendingIterator();
		while (iter.hasNext()) {
			JunctionExtra ex = iter.next();
			ex.afterOnMessageReceived(header, message);
		}
	}
	
	public boolean beforeSendMessageToActor(String actorID, JSONObject message) {
		Iterator<JunctionExtra>iter = mExtras.iterator();
		while (iter.hasNext()) {
			JunctionExtra ex = iter.next();
			if (!ex.beforeSendMessageToActor(actorID, message))
				return false;
		}
		return true;
	}
	
	public boolean beforeSendMessageToRole(String role, JSONObject message) {
		Iterator<JunctionExtra>iter = mExtras.iterator();
		while (iter.hasNext()) {
			JunctionExtra ex = iter.next();
			if (!ex.beforeSendMessageToRole(role, message))
				return false;
		}
		return true;
	}
	
	public boolean beforeSendMessageToSession(JSONObject message) {
		Iterator<JunctionExtra>iter = mExtras.iterator();
		while (iter.hasNext()) {
			JunctionExtra ex = iter.next();
			if (!ex.beforeSendMessageToSession(message))
				return false;
		}
		return true;
	}
	
	/**
	 * Adds an Extra to the set of executed extras.
	 * @param extra
	 */
	public void registerExtra(JunctionExtra extra) {
		mExtras.add(extra);
	}
}
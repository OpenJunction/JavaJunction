package edu.stanford.prpl.junction.api.activity;


/**
 * Might want to remove this class and just use Strings.
 *
 */


public abstract class JunctionRole {
	public abstract String getRoleID();
	public String toString() { return getRoleID(); }
}

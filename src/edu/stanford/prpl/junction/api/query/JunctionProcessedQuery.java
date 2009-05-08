package edu.stanford.prpl.junction.api.query;

public interface JunctionProcessedQuery extends JunctionQuery {
	public boolean isPersistent();
	// public JunctionEnvironment getEnvironment();
	// or something... needs to encompass:
	// user, session, etc
}

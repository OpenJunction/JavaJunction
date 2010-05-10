package edu.stanford.junction.props;


public abstract class PropStateOperation implements IPropStateOperation{

	private long nonce;

	public PropStateOperation(long nonce){
		this.nonce = nonce;
	}

	public long getNonce() {
		return nonce;
	}

}

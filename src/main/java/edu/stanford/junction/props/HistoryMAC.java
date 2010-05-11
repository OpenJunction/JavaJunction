package edu.stanford.junction.props;

class HistoryMAC{
	public long stateNumber;
	public String stateHash;
	public HistoryMAC(long stateNumber, String stateHash){
		this.stateNumber = stateNumber;
		this.stateHash = stateHash;
	}

	public String toString(){
		return "[" + stateNumber + ":" + stateHash + "]";
	}
}


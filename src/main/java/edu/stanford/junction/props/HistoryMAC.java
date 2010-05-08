package edu.stanford.junction.props;

class HistoryMAC{
	public int stateNumber;
	public String stateHash;
	public HistoryMAC(int stateNumber, String stateHash){
		this.stateNumber = stateNumber;
		this.stateHash = stateHash;
	}

	public String toString(){
		return "[" + stateNumber + ":" + stateHash + "]";
	}
}


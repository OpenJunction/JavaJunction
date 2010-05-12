package edu.stanford.junction.props;

class HistoryMAC{
	public long stateNum;
	public String stateHash;
	public HistoryMAC(long stateNum, String stateHash){
		this.stateNum = stateNum;
		this.stateHash = stateHash;
	}

	public String toString(){
		return "[" + stateNum + ":" + stateHash + "]";
	}
}


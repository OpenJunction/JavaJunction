package edu.stanford.junction.props;

class HistoryMAC{
	public long seqNum;
	public String stateHash;
	public HistoryMAC(long seqNum, String stateHash){
		this.seqNum = seqNum;
		this.stateHash = stateHash;
	}

	public String toString(){
		return "[" + seqNum + ":" + stateHash + "]";
	}
}


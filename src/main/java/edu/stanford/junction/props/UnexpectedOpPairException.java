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
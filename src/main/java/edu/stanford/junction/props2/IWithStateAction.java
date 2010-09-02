package edu.stanford.junction.props2;

public interface IWithStateAction<T>{
	T run(IPropState state);
}

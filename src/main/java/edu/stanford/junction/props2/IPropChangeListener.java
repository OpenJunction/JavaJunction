package edu.stanford.junction.props2;

public interface IPropChangeListener  {
	String getType();
	void onChange(Object data);
}
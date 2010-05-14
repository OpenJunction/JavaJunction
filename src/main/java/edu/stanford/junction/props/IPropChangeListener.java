package edu.stanford.junction.props;

public interface IPropChangeListener  {
	String getType();
	void onChange(Object data);
}
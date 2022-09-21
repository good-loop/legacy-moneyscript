package com.winterwell.moneyscript.output;

public class RuleException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public RuleException(String string, Throwable ex) {
		super(string, ex);
	}

}

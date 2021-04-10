package com.winterwell.moneyscript.lang;

import com.winterwell.moneyscript.lang.num.Numerical;

/**
 * Marks an error in the spreadsheet. Like a NaN with a comment
 * @author daniel
 *
 */
public class ErrorNumerical extends Numerical {
	
	private static final long serialVersionUID = 1L;
	private Throwable ex;
	
	public ErrorNumerical(Throwable ex) {
		super(Double.NaN);
		this.ex = ex;
	}
	
	@Override
	public String toString() {				
		return "Error"; //+ex.getMessage();
	}

}

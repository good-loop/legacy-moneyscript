package com.winterwell.moneyscript.lang;

import com.winterwell.maths.stats.distributions.d1.Gaussian1D;
import com.winterwell.maths.stats.distributions.d1.IDistribution1D;
import com.winterwell.moneyscript.lang.num.Numerical;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Particles1D;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.StrUtils;

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

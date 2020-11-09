package com.winterwell.moneyscript.lang.num;

import java.util.Collections;
import java.util.Set;

import com.winterwell.maths.stats.distributions.d1.IDistribution1D;
import com.winterwell.moneyscript.lang.UncertainNumerical;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Cell;

public abstract class Formula {

	/**
	 * Convert distributions into samples. 
	 * @param n
	 * @return
	 */
	public static final Numerical sample(Numerical n) {
		if (n instanceof UncertainNumerical) {
			UncertainNumerical un = (UncertainNumerical) n;
			Numerical n2 = un.sample();
			return n2;
		}
		// just a number
		return n;
	}
	
	String op;
	
	public Formula(String op) {
		assert op != null;
		this.op = op.intern(); // allow for == comparisons
	}
		
	public abstract Numerical calculate(Cell b);

	/**
	 * true for rules such as "* 2" which stack with previous rules.
	 * false normally
	 * Used for correctness checking.
	 * @return
	 */
	public boolean isStacked() {
		return false;
	}

	/**
	 * 
	 * @param focus Can be null
	 * @return
	 */
	public Set<String> getRowNames(Cell focus) {
		return Collections.emptySet();
	}
	
}

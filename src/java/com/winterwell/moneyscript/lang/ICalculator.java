package com.winterwell.moneyscript.lang;

import com.winterwell.moneyscript.lang.num.Numerical;

public interface ICalculator {

	/**
	 * 
	 * @param x
	 * @param y
	 * @return x / y
	 */
	Numerical divide(Numerical x, Numerical y);
	
	Numerical plus(Numerical x, Numerical y);
	
	Numerical times(Numerical x, Numerical y);

	Numerical timesScalar(Numerical x, double y);
	
	/**
	 * 
	 * @param a
	 * @param b
	 * @return a - b
	 */
	Numerical minus(Numerical a, Numerical b);

	
}

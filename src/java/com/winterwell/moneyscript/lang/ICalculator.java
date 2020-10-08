package com.winterwell.moneyscript.lang;

import com.winterwell.moneyscript.lang.num.Numerical;

public interface ICalculator {

	Numerical divide(Numerical x, Numerical y);
	
	Numerical plus(Numerical x, Numerical y);
	
	Numerical times(Numerical x, Numerical y);

	Numerical minus(Numerical numerical, Numerical b);
	
}

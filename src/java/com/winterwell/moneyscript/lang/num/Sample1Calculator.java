package com.winterwell.moneyscript.lang.num;

import com.winterwell.moneyscript.lang.ICalculator;
import com.winterwell.moneyscript.lang.UncertainNumerical;
import com.winterwell.moneyscript.output.Business;

/**
 * 
 * @author daniel
 * @testedby  ParticleCloudCalculatorTest}
 */
public final class Sample1Calculator implements ICalculator {

	final DefaultCalculator simple = new DefaultCalculator();
	
	@Override
	public Numerical divide(Numerical x, Numerical y) {
		x = sample(x);
		y = sample(y);
		return simple.divide(x, y);		
	}

	@Override
	public Numerical minus(Numerical x, Numerical y) {
		x = sample(x);
		y = sample(y);
		return simple.minus(x, y);		
	}

	@Override
	public Numerical plus(Numerical x, Numerical y) {
		x = sample(x);
		y = sample(y);
		return simple.plus(x, y);		
	}

	@Override
	public Numerical times(Numerical x, Numerical y) {
		x = sample(x);
		y = sample(y);
		return simple.times(x, y);		
	}
	
	@Override
	public Numerical timesScalar(Numerical x, double y) {
		x = sample(x);
		return simple.timesScalar(x, y);
	}

	private Numerical sample(Numerical x) {
		if (x==Business.EVALUATING) {
			throw new LoopyEvaluationException("Cannot sample EVALUATING "+this);
		}
		if (x instanceof UncertainNumerical) {			
			return ((UncertainNumerical) x).sample();
		}
		return x;
	}

	@Override
	public String toString() {
		return "Sample1Calculator [simple=" + simple + "]";
	}

}
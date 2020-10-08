package com.winterwell.moneyscript.lang.num;

import com.winterwell.maths.IScalarArithmetic;
import com.winterwell.maths.stats.distributions.d1.Constant1D;
import com.winterwell.maths.stats.distributions.d1.IDistribution1D;
import com.winterwell.moneyscript.lang.ICalculator;
import com.winterwell.moneyscript.lang.ParticleCloudCalculatorTest;
import com.winterwell.moneyscript.lang.UncertainNumerical;
import com.winterwell.moneyscript.output.Business;

/**
 * 
 * @author daniel
 * @testedby {@link ParticleCloudCalculatorTest}
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

	private Numerical sample(Numerical x) {
		if (x==Business.EVALUATING) {
			throw new LoopyEvaluationException("Cannot sample EVALUATING "+this);
		}
		if (x instanceof UncertainNumerical) {
			IDistribution1D dist = ((UncertainNumerical) x).getDist();
			Double v = dist.sample();
			return new Numerical(v, x.getUnit());
		}
		return x;
	}

	@Override
	public String toString() {
		return "Sample1Calculator [simple=" + simple + "]";
	}

}
package com.winterwell.moneyscript.lang.num;

import com.winterwell.maths.IScalarArithmetic;
import com.winterwell.maths.stats.distributions.d1.Constant1D;
import com.winterwell.maths.stats.distributions.d1.IDistribution1D;
import com.winterwell.moneyscript.lang.ICalculator;
import com.winterwell.moneyscript.lang.UncertainNumerical;
import com.winterwell.moneyscript.output.Particles1D;

interface IOp {

	double calc(Double sx, Double sy);
	
}

/**
 * 
 * @author daniel
 * @testedby ParticleCloudCalculatorTest
 */
public final class ParticleCloudCalculator implements ICalculator {

	private static final IOp DIVIDE = new IOp() {			
		@Override
		public double calc(Double x, Double y) {
			return x/y;
		}
	};
	private static final IOp MINUS = new IOp() {			
		@Override
		public double calc(Double x, Double y) {
			return x - y;
		}
	};
	private static final IOp PLUS = new IOp() {			
		@Override
		public double calc(Double x, Double y) {
			return x + y;
		}
	};
	private static final int SAMPLES = 10000;
	private static final IOp TIMES = new IOp() {			
		@Override
		public double calc(Double x, Double y) {
			return x * y;
		}
	};
	
	final DefaultCalculator simple = new DefaultCalculator();
	
	private IDistribution1D dist(Numerical x) {
		if (x instanceof UncertainNumerical) {
			IDistribution1D dist = ((UncertainNumerical)x).getDist();
			return dist;
		} 
		assert x.getClass() == Numerical.class;
		return new Constant1D(x.doubleValue());
	}

	@Override
	public Numerical divide(Numerical x, Numerical y) {
		if (isSimple(x,y)) {
			return simple.divide(x, y);
		}
		return sample(x, y, DIVIDE);
	}

	private boolean isSimple(Numerical x, Numerical y) {
		if (x instanceof UncertainNumerical) return false;
		if (y instanceof UncertainNumerical) return false;
		return true;
	}

	@Override
	public Numerical minus(Numerical x, Numerical y) {
		if (isSimple(x,y)) {
			return simple.minus(x, y);
		}
		return sample(x, y, MINUS);
	}

	@Override
	public Numerical plus(Numerical x, Numerical y) {
		if (isSimple(x,y)) {
			return simple.plus(x, y);
		}
		return sample(x, y, PLUS);		
	}

	private Numerical sample(Numerical x, Numerical y, IOp op) {
		// special case: constant * some dists
		IDistribution1D dx = dist(x);
		IDistribution1D dy = dist(y);
		String unit = simple.unit(x, y);
		IDistribution1D exact = sample2_exact(dx,dy,op);
		if (exact!=null) {
			return new UncertainNumerical(exact, unit);
		}
		double[] pts = new double[SAMPLES];
		for(int i=0; i<SAMPLES; i++) {
			Double sx = dx.sample();
			Double sy = dy.sample();
			pts[i] = op.calc(sx, sy);
		}
		Particles1D pc = new Particles1D(pts);		
		return new UncertainNumerical(pc, unit);
	}

	private IDistribution1D sample2_exact(IDistribution1D dx, IDistribution1D dy, IOp op) {
		if (dx instanceof Constant1D && dy instanceof IScalarArithmetic) {
			double x = dx.getMean();
			IScalarArithmetic scalable = (IScalarArithmetic) dy;
			if (op==TIMES) {
				return (IDistribution1D) scalable.times(x);
			} else if (op==PLUS) {
				return (IDistribution1D) scalable.plus(x);
			} else if (op==MINUS) {
				return (IDistribution1D) scalable.times(-1).plus(x);
			} else if (op==DIVIDE) {
				// no can do
				return null;
			}			
		}
		if (dy instanceof Constant1D && dx instanceof IScalarArithmetic) {
			double y = dy.getMean();
			IScalarArithmetic scalable = (IScalarArithmetic) dx;
			if (op==TIMES) {
				return (IDistribution1D) scalable.times(y);
			} else if (op==PLUS) {
				return (IDistribution1D) scalable.plus(y);
			} else if (op==MINUS) {
				return (IDistribution1D) scalable.plus(-y);
			} else if (op==DIVIDE) {
				return (IDistribution1D) scalable.times(1/y);
			}			
		}		
		return null;
	}

	@Override
	public Numerical times(Numerical x, Numerical y) {
		if (isSimple(x,y)) {
			return simple.times(x, y);
		}
		return sample(x, y, TIMES);
	}

}
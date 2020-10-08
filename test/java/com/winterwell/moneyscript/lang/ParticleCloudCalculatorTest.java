package com.winterwell.moneyscript.lang;

import org.junit.Test;

import com.winterwell.maths.GridInfo;
import com.winterwell.maths.stats.distributions.d1.GridDistribution1D;
import com.winterwell.maths.stats.distributions.d1.IDistribution1D;
import com.winterwell.maths.stats.distributions.d1.UniformDistribution1D;
import com.winterwell.moneyscript.lang.num.Numerical;
import com.winterwell.moneyscript.lang.num.ParticleCloudCalculator;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;
import com.winterwell.utils.containers.Range;

public class ParticleCloudCalculatorTest {

	@Test
	public void testDivide() {
		ParticleCloudCalculator calc = new ParticleCloudCalculator();
		{
			Numerical y = new Numerical(-2, "£");
			Numerical x = new Numerical(10);
			Numerical xy = calc.divide(x, y);
			assert xy.doubleValue() == -5;
			assert xy.getUnit().equals("£");
			assert ! (xy instanceof UncertainNumerical);
		}
		{
			Numerical y = new Numerical(-2);
			UncertainNumerical x = new UncertainNumerical(new UniformDistribution1D(new Range(8,12)), "£");
			double vx = x.getDist().getVariance();
			UncertainNumerical xy = (UncertainNumerical) calc.divide(x, y);
			Printer.out(x+" / "+y+" = "+xy);
			assert xy.getUnit().equals("£");
			IDistribution1D xyd = xy.getDist();
			Printer.out(xyd);
			assert MathUtils.approx(xyd.getMean(), -5);
			double var = xyd.getVariance();
			assert MathUtils.approx(var, vx/4);
			double sd = xyd.getStdDev();
		}
	}

	@Test
	public void testPlus() {
		ParticleCloudCalculator calc = new ParticleCloudCalculator();
		{
			UncertainNumerical x = new UncertainNumerical(new UniformDistribution1D(new Range(0,2)), null);
			UncertainNumerical y = new UncertainNumerical(new UniformDistribution1D(new Range(8,12)), "£");			
			double vx = x.getDist().getVariance();
			double vy = y.getDist().getVariance();
			UncertainNumerical xy = (UncertainNumerical) calc.plus(x, y);
			Printer.out(x+" + "+y+" = "+xy);
			Printer.out(xy.toString());
			assert xy.getUnit().equals("£");
			IDistribution1D xyd = xy.getDist();
			Printer.out(xyd);
			assert MathUtils.approx(xyd.getMean(), 11);
			double var = xyd.getVariance();
			assert MathUtils.approx(var, vx + vy);
			double sd = xyd.getStdDev();
		}
		{	// dice
			UncertainNumerical x = new UncertainNumerical(new UniformDistribution1D(new Range(1,6)), null);
			UncertainNumerical y = new UncertainNumerical(new UniformDistribution1D(new Range(1,6)), null);			
			double vx = x.getDist().getVariance();
			double vy = y.getDist().getVariance();
			UncertainNumerical xy = (UncertainNumerical) calc.plus(x, y);
			Printer.out(x+" + "+y+" = "+xy);
//			ARender render = new RenderWithFlot();
			// convert away from point masses
			GridDistribution1D grid = new GridDistribution1D(new GridInfo(1, 12, 12));
			for(int i=0; i<10000; i++) {
				grid.count(xy.getDist().sample());
			}
			grid.normalise();
//			AChart chart = new Distribution1DChart(grid);
//			render.renderAndPopupAndBlock(chart);
		}
	}

}

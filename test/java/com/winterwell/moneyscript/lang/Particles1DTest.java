package com.winterwell.moneyscript.lang;

import static org.junit.Assert.*;

import org.junit.Test;

import com.winterwell.maths.stats.distributions.d1.UniformDistribution1D;
import com.winterwell.moneyscript.output.Particles1D;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.containers.Range;

public class Particles1DTest {

	@Test
	public void testGetConfidence() {
		double[] pts = new double[10000];
		UniformDistribution1D uni = new UniformDistribution1D(new Range(100, 1100));
		for(int i=0; i<pts.length; i++) {
			pts[i] = uni.sample();
		}
		Particles1D ps = new Particles1D(pts);
		
		double lo = ps.getConfidence(0.05);
		double m = ps.getConfidence(0.5);
		double hi = ps.getConfidence(0.95);
		assert MathUtils.approx(lo, 150) : lo;
		assert MathUtils.approx(m, 600) : m;
		assert MathUtils.approx(hi, 1050) : hi;
		
	}

}

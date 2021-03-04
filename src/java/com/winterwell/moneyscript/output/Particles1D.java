/**
 * 
 */
package com.winterwell.moneyscript.output;

import java.util.Arrays;
import java.util.Collections;

import com.winterwell.maths.IScalarArithmetic;
import com.winterwell.maths.stats.StatsUtils;
import com.winterwell.maths.stats.distributions.d1.ADistribution1D;
import com.winterwell.maths.stats.distributions.d1.IDistribution1D;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.containers.Range;

/**
 * A real-valued particle cloud for Monte-Carlo calculations.
 * @author daniel
 *
 */
public class Particles1D extends ADistribution1D implements IScalarArithmetic {

	
	@Override
	public Range getSupport() {
		return new Range(MathUtils.min(pts), MathUtils.max(pts));
	}
	
	double[] pts;
	
	/**
	 * @testedby  Particles1DTest#testGetConfidence()}
	 */
	@Override
	public synchronized double getConfidence(double totalWeight) {
		assert MathUtils.isProb(totalWeight);		
		if (totalWeight==0) return Double.NEGATIVE_INFINITY;
		int cnt = (int) Math.round(pts.length * totalWeight);
		Collections.sort(Containers.asList(pts));
		return pts[cnt];
	}
	
	public Particles1D(double[] pts) {
		this.pts = pts;
	}	
	
	public Particles1D(int numParticles) {
		this(new double[numParticles]);
	}

	public Particles1D(int numParticles, IDistribution1D dist) {
		// exact copy?
		if (dist instanceof Particles1D) {
			if (numParticles == ((Particles1D) dist).pts.length) {
				this.pts = Arrays.copyOf(pts, numParticles);
				return;
			}
		}
		// sample
		this.pts = new double[numParticles];
		for(int i=0; i<numParticles; i++) {
			Double v = dist.sample();
			if ( ! Double.isFinite(v)) {
				throw new IllegalArgumentException(v+" in "+dist);
			}
			pts[i] = v;
		}
	}

	public synchronized void add(double pt) {
		pts = Arrays.copyOf(pts, pts.length+1);
		pts[pts.length-1] = pt;
//		return new Particles1D(pts2);
	}
	
	@Override
	public double getVariance() {
		return StatsUtils.var(pts);
	}
	
	@Override
	public Double sample() {
		int i = random().nextInt(pts.length);
		return pts[i];
	}

	@Override
	public double getMean() {
		return StatsUtils.mean(pts);
	}

	/**
	 * Density at a point: either infinite or 0
	 * TODO have a bump function option which isn't quite so stark. 
	 */
	@Override
	public double density(double x) {
		for(double p : pts) {
			if (x==p) return Double.POSITIVE_INFINITY;
		}
		return 0;
	}

	@Override
	public IScalarArithmetic plus(double x) {
		double[] pts2 = new double[pts.length];
		for (int i = 0; i < pts2.length; i++) {
			pts2[i] = pts[i] + x;
		}
		return new Particles1D(pts2);
	}

	@Override
	public IScalarArithmetic times(double x) {
		double[] pts2 = new double[pts.length];
		for (int i = 0; i < pts2.length; i++) {
			pts2[i] = pts[i] * x;
		}
		return new Particles1D(pts2);
	}

	public double[] getPoints() {
		return pts;
	}

}

package com.winterwell.moneyscript.lang;

import com.winterwell.maths.stats.distributions.d1.Gaussian1D;
import com.winterwell.maths.stats.distributions.d1.IDistribution1D;
import com.winterwell.moneyscript.lang.num.Numerical;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Particles1D;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.StrUtils;

public class UncertainNumerical extends Numerical {
	
	private static final long serialVersionUID = 1L;
	private IDistribution1D dist;
	
	public IDistribution1D getDist() {
		return dist;
	}
	
	public UncertainNumerical(IDistribution1D dist) {
		this(dist, null);
	}
	public UncertainNumerical(IDistribution1D dist, String unit) {
		super(Double.NaN, unit); // send NaN down to help flag any use of it
		this.dist = dist;
	}
	
	@Override
	public double doubleValue() {
		return dist.getMean();
	}
	
//	double LOW_CONFIDENCE = 0.25;
//	double HIGH_CONFIDENCE = 0.75;
	
	@Override
	public String toString() {		
		if (dist==null) {
			return super.toString(); // NB: can happen during new() with debug
		}
		if (dist instanceof Particles1D) {
			double m = dist.getMean();
			double sd = dist.getStdDev();
			if (MathUtils.isTooSmall(sd)) {
				return super.toString();
			}
//			double errLo = dist.getConfidence(LOW_CONFIDENCE); 
//			double errHi = dist.getConfidence(HIGH_CONFIDENCE);
			double err = Math.abs(sd/m);
			// Use absolute +- for divide-by-zero, or just silly-large %
			if ( ! MathUtils.isFinite(err) || Math.abs(err) > 2.5) {
				return super.toString()+" ± ~"+StrUtils.toNSigFigs(sd, 2);	
			}
			return super.toString()+" ± ~"+StrUtils.toNSigFigs(100*err,2)+"%";
		}
		// hack - std-dev is more useful than variance for human info
		if (dist instanceof Gaussian1D) {
			double m = dist.getMean();
			double sd = dist.getStdDev();
			return "N("+super.toString()+", sd:"+StrUtils.toNSigFigs(sd, 2)+")";
		}
		return (getUnit()==null? "":getUnit()) + dist.toString();
	}

	/**
	 * Random sample if sampling OR return the mean if Business.settings.samples = 1
	 * @return
	 */
	public Numerical sample() {
		Numerical n;
		// HACK sampling??
		Business b = Business.get();		
		int samples = b.getSettings().getSamples();
		if (samples < 2) {
			// no samples - use the mean for robust sensible answers
			n = new Numerical(dist.getMean(), getUnit());			
		} else {
			Double x = dist.sample();
			n = new Numerical(x, getUnit());
		}
		// pass along the distro info in a comment (it's better than nothing)
		n.comment = StrUtils.joinWithSkip(". ", comment, toString());
		return n;
	}

}

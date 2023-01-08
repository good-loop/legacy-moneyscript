package com.winterwell.moneyscript.lang.num;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.winterwell.moneyscript.lang.ErrorNumerical;
import com.winterwell.moneyscript.lang.ICalculator;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.log.Log;

/**
 * @testedby {@link DefaultCalculatorTest}
 * @author daniel
 *
 */
final class DefaultCalculator implements ICalculator {

	/**
	 * How do tags work here? What is e.g. £10={#red:£10} / 2={#red:1,#blue:1}?
	 * Do we combine tags to get blue-red?
	 * Let's assume one exclusive tagset, so the sum above would be £5{#red:10}
	 * divide-by-0 results in tag tracking being dropped for that tag
	 */
	@Override
	public Numerical divide(Numerical x, Numerical y) {
		assert x != null && y != null : x+" "+y;
		if (x instanceof ErrorNumerical) return x;
		if (y instanceof ErrorNumerical) return y;
		if (Numerical.isZero(x)) {
			// handles 0 / 0 as 0
			return new Numerical(0, unit(x,y));
		}
		Numerical n = new Numerical(x.doubleValue() / y.doubleValue(), unit(x, y));
		// tags
		if (x.value4tag!=null) {
			n.value4tag = new ArrayMap();
			for(Map.Entry<String,Double> e : x.value4tag.entrySet()) {
				Double xv = e.getValue();
				if (xv == 0.0) {
					continue; // no need to track 0s
				}
				String tag = e.getKey();
				if (y.value4tag == null) {
					double v = xv / y.doubleValue();
					if (MathUtils.isFinite(v)) {
						n.value4tag.put(tag, v);
					}
					continue;
				}
				// tagA / tagB
				Double yv = y.value4tag.get(tag);
				if (yv==null) {
					double residual = untaggedResidual(y, tag);
					yv = residual; // can be zero!
				}
				double v = xv / yv;
				if (MathUtils.isFinite(v)) {
					n.value4tag.put(tag, v);
				}
			}
			// tagset logic -- allow e.g. colour.red and product.truck to mix - part two
			// handle any tagset.tags in y that may have got missed 
			if (y.value4tag != null) {
				for(Map.Entry<String,Double> e : y.value4tag.entrySet()) {
					Double yv = e.getValue();
					String tag = e.getKey();
					if (tag.indexOf('.')==-1) continue; // done already in the previous for loop
					if (x.value4tag.containsKey(tag)) continue; // done already in the previous for loop
					double residual = untaggedResidual(x, tag);
					double v = residual / yv;	
					if (MathUtils.isFinite(v)) {					
						n.value4tag.put(tag, v);
					}
				}
			}
		} else if (y.value4tag!=null) {
			n.value4tag = new ArrayMap();
			for(Map.Entry<String,Double> e : y.value4tag.entrySet()) {
				Double yv = e.getValue();
				double v = x.doubleValue() / yv;
				if (MathUtils.isFinite(v)) {
					n.value4tag.put(e.getKey(), v);
				}
			}
		}
		return n;
	}
	
	
	@Override
	public Numerical plus(Numerical x, Numerical y) {
		return plus2(x, 1, y);
	}
	
	Numerical plus2(Numerical x, double b, Numerical y) {
		if (x instanceof ErrorNumerical) return x;
		if (y instanceof ErrorNumerical) return y;
		Numerical n = new Numerical(x.doubleValue() + b*y.doubleValue(), unit(x, y));
		// tags
		if (x.value4tag!=null) {
			n.value4tag = new ArrayMap(x.value4tag);
		}
		if (y.value4tag!=null) {
			if (n.value4tag==null) n.value4tag = new ArrayMap();
			Set<Entry<String, Double>> es = y.value4tag.entrySet();
			for (Entry<String, Double> entry : es) {
				Containers.plus(n.value4tag, entry.getKey(), entry.getValue()*b); 
			}
		}
		// excel -- done higher up in Formula to potentially be a bit smarter		
		return n;		
	}

	/**
	 * How do tags work here? What is e.g. £10={#red:£10} * 2={#red:1,#blue:1}?
	 * Do we combine tags to get blue-red?
	 * Let's assume one exclusive tagset, so the sum above would be £20{#red:10,#blue:0}
	 */
	@Override
	public Numerical times(Numerical x, Numerical y) {
		assert x != null && y != null : x+" "+y;
		if (x instanceof ErrorNumerical) return x;
		if (y instanceof ErrorNumerical) return y;
		Numerical n = new Numerical(x.doubleValue() * y.doubleValue(), unit(x, y));
		// tags
		if (x.value4tag!=null) {
			n.value4tag = new ArrayMap();
			for(Map.Entry<String,Double> e : x.value4tag.entrySet()) {
				Double xv = e.getValue();
				if (y.value4tag == null) {
					// just one side is tagged -- carry the tags across
					n.value4tag.put(e.getKey(), xv * y.doubleValue());
					continue;
				}
				// tagA * tagB
				String tag = e.getKey();
				Double yv = y.value4tag.get(tag);
				if (yv==null) {
					double residual = untaggedResidual(y, tag);
					if (residual==0) continue;
					yv = residual;
				}
				n.value4tag.put(tag, xv * yv);				
			}
			// tagset logic -- allow e.g. colour.red and product.truck to mix - part two
			// handle any tagset.tags in y that may have got missed 
			if (y.value4tag != null) {
				for(Map.Entry<String,Double> e : y.value4tag.entrySet()) {
					Double yv = e.getValue();
					String tag = e.getKey();
					if (tag.indexOf('.')==-1) continue; // done already in the previous for loop
					if (x.value4tag.containsKey(tag)) continue; // done already in the previous for loop
					double residual = untaggedResidual(x, tag);
					if (residual==0) continue;					
					n.value4tag.put(tag, residual * yv);				
				}
			}
		} else if (y.value4tag!=null) {
			// just one side is tagged -- carry the tags across
			n.value4tag = new ArrayMap();
			for(Map.Entry<String,Double> e : y.value4tag.entrySet()) {
				Double yv = e.getValue();
				n.value4tag.put(e.getKey(), yv * x.doubleValue());							
			}			
		}
		return n;
	}


	/**
	 * - Allow 2#green * 3 = 6#green
	 * - Tagset logic -- allow e.g. colour.red and product.truck to mix.
	 * @param y
	 * @param tag
	 * @return the untagged (wrt tag's tagset if it has one) residual of y
	 */
	private double untaggedResidual(Numerical y, String tag) {
		double sumTaggedY = 0; 
		String tagset = tag.substring(0, tag.indexOf('.')+1);								
		for(String ytag : y.value4tag.keySet()) {
			if (ytag.startsWith(tagset)) {
				if (tagset.isEmpty() && ytag.indexOf('.') != -1) {
					continue;
				}
				sumTaggedY += y.value4tag.get(ytag);
			}
		}					
		double residual = y.doubleValue() - sumTaggedY;
		return residual;
	}
	

	@Override
	public Numerical timesScalar(Numerical x, double y) {
		assert x != null : x+" "+y;
		if (x instanceof ErrorNumerical) return x;
		Numerical n = new Numerical(x.doubleValue() * y, x.getUnit());
		// tags
		if (x.value4tag!=null) {
			n.value4tag = new ArrayMap();
			for(Map.Entry<String,Double> e : x.value4tag.entrySet()) {
				n.value4tag.put(e.getKey(), e.getValue() * y);
			}
		}
		return n;
	}

	public static String unit(Numerical a, Numerical b) {
		// anything beats % -- even unset! 
		// So that 10% * Customers is not itself a %, even though Customers probably doesnt have a unit defined
		if ("%".equals(a.getUnit())) return b.getUnit();
		if ("%".equals(b.getUnit())) return a.getUnit();
		// pick the non-blank
		if (b.getUnit() == null) return a.getUnit();
		if (a.getUnit() == null) return b.getUnit();
		// both are set
		// ...clash?
		if ( ! a.getUnit().equals(b.getUnit())) {			
			Log.w("Numerical", "Unit mismatch: "+a.getUnit()+" != "+b.getUnit());
			return null;
		}
		return a.getUnit();
	}

	@Override
	public Numerical minus(Numerical x, Numerical y) {
		return plus2(x, -1, y);
	}

}
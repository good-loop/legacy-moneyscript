package com.winterwell.moneyscript.lang.num;

import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.winterwell.moneyscript.lang.ErrorNumerical;
import com.winterwell.moneyscript.lang.ICalculator;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.log.Log;

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
				if (y.value4tag == null) {
					double v = xv / y.doubleValue();
					if (MathUtils.isFinite(v)) {
						n.value4tag.put(e.getKey(), v);
					}
					continue;
				}
				// tagA / tagB
				Double yv = y.value4tag.get(e.getKey());
				if (yv==null) {
					double sumTaggedY = MathUtils.sum(y.value4tag.values());
					double residual = y.doubleValue() - sumTaggedY;
					yv = residual; // can be zero!
				}
				double v = xv / yv;
				if (MathUtils.isFinite(v)) {
					n.value4tag.put(e.getKey(), v);
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
					n.value4tag.put(e.getKey(), xv * y.doubleValue());
					continue;
				}
				// tagA * tagB
				Double yv = y.value4tag.get(e.getKey());
				if (yv==null) {
					double sumTaggedY = MathUtils.sum(y.value4tag.values());
					double residual = y.doubleValue() - sumTaggedY;
					if (residual==0) continue;
					yv = residual;
				}
				n.value4tag.put(e.getKey(), xv * yv);				
			}
		} else if (y.value4tag!=null) {
			n.value4tag = new ArrayMap();
			for(Map.Entry<String,Double> e : y.value4tag.entrySet()) {
				Double yv = e.getValue();
				n.value4tag.put(e.getKey(), yv * x.doubleValue());							
			}			
		}
		return n;
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
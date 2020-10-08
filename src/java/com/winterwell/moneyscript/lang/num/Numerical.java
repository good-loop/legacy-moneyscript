package com.winterwell.moneyscript.lang.num;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.winterwell.maths.IScalarArithmetic;
import com.winterwell.moneyscript.lang.ICalculator;
import com.winterwell.moneyscript.lang.NumericalTest;
import com.winterwell.moneyscript.lang.UncertainNumerical;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.log.Log;

/**
 * A number optionally plus a unit.
 * Has a flexible String constructor.
 * @testedby {@link NumericalTest}
 * @author daniel
 *
 */
public class Numerical extends Number implements IScalarArithmetic {

	public static boolean isZero(Numerical x) {
		if (x==null) return true;
		if (x instanceof UncertainNumerical) {
			// TODO handle [-1,1] != 0
		}
		return x.doubleValue()==0;
	}
	
	protected static final ICalculator calc = new Sample1Calculator(); 
	
	private static final long serialVersionUID = 1L;

	/**
	 * zero, but 'cos it's no value
	 */
	public static final Numerical NULL = new Numerical(0);
	
	private final Double value;
	
	public String comment;
	
	/**
	 * treat % as a unit - but one that gets over-ridden by anything else
	 */
	private final String unit;

	// TODO remove the ,?
	public static Pattern number = Pattern.compile("-?(£|$)?([0-9]+[0-9,]+[0-9]{3}|[0-9]+\\.[0-9]+|[0-9]+)(k|m|bn)?%?", Pattern.CASE_INSENSITIVE);
	
	/**
	 * @param s Accepts a range of things: £10, -£20, $5, 0.01, 1,000 5k, 5M, 1.5bn, 20%
	 */
	public Numerical(String s) {		
		Matcher m = number.matcher(s);
		if ( ! m.matches()) throw new IllegalArgumentException(s);
		Double v = Double.valueOf(m.group(2).replace(",", ""));
		String mk = m.group(3);
		if ("m".equals(mk)) {
			v *= 1000000;
		}
		if ("k".equals(mk)) {
			v *= 1000;
		}
		if ("bn".equals(mk)) {
			v *= 1000000000;
		}
		if (m.group().charAt(0) == '-') {
			v = -v;
		}	
		if (s.contains("%")) {
			v = v/100;
		}
		value = v;
		if (m.group(1) != null) {
			unit = m.group(1).intern();
		} else {
			unit = s.contains("%")? "%" : null;
		}
	}
	
	public Numerical(Number value) {
		this(value.doubleValue(), null);
	}



	public Numerical(double value, String unit) {
		this.value = value;
		assert MathUtils.isFinite(value) || getClass() != Numerical.class : value;
		this.unit = unit==null? null : unit.intern();		
	}

	@Override
	public String toString() {
		String sign = doubleValue() < 0? "-" : "";
		double v = Math.abs(doubleValue());
		if ("%".equals(unit)) {
			v *= 100;
		}
		String num;
		if (v>1000000) {
			num = StrUtils.toNSigFigs(v/1000000, 2)+"M";
		} else if (v>1000) {
			num = StrUtils.toNSigFigs(v/1000, 2)+"k";
		} else {
			num = StrUtils.toNSigFigs(v, 2);
		}
		if (unit==null) {
			return sign+num;	
		}
		if ("%".equals(unit)) {
			return sign+num+'%';
		}
		return sign+unit+num;
	}



	@Override
	public int intValue() {
		return (int) doubleValue();
	}



	@Override
	public long longValue() {
		return (long) doubleValue();
	}



	@Override
	public float floatValue() {
		return (float) doubleValue();
	}



	@Override
	public double doubleValue() {
		return value.doubleValue();
	}

	public static boolean isNumerical(String rTxt) {
		try {
			new Numerical(rTxt);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public Numerical plus(Numerical b) {
		if (b==null) return this;
		return calc.plus(this, b);		
	}


	public Numerical divide(Numerical b) {
		return calc.divide(this, b);
	}

	public String getUnit() {
		return unit;
	}

	public Numerical times(Numerical b) {
		return calc.times(this, b);
	}

	public Numerical times(double b) {
		return calc.times(this, new Numerical(b));
	}

	public Numerical minus(Numerical b) {
		return calc.minus(this, b);
	}

	@Override
	public IScalarArithmetic plus(double b) {
		return calc.plus(this, new Numerical(b));
	}

}


final class DefaultCalculator implements ICalculator {

	@Override
	public Numerical divide(Numerical x, Numerical y) {
		return new Numerical(x.doubleValue() / y.doubleValue(), unit(x, y));
	}

	@Override
	public Numerical plus(Numerical x, Numerical y) {
		return new Numerical(x.doubleValue() + y.doubleValue(), unit(x, y));
	}

	@Override
	public Numerical times(Numerical x, Numerical y) {
		assert x != null && y != null : x+" "+y;
		return new Numerical(x.doubleValue() * y.doubleValue(), unit(x, y));
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
		return new Numerical(x.doubleValue() - y.doubleValue(), unit(x, y));
	}

}

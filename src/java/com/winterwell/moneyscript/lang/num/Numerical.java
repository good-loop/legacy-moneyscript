package com.winterwell.moneyscript.lang.num;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.winterwell.maths.IScalarArithmetic;
import com.winterwell.maths.stats.distributions.discrete.ObjectDistribution;
import com.winterwell.moneyscript.lang.ICalculator;
import com.winterwell.moneyscript.lang.UncertainNumerical;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.containers.ArrayMap;

/**
 * A number optionally plus a unit.
 * Has a flexible String constructor.
 * @testedby NumericalTest
 * @author daniel
 *
 */
public class Numerical extends Number implements IScalarArithmetic {

	/**
	 * 
	 * @param x Can be null
	 * @return
	 */
	public static boolean isZero(Numerical x) {
		if (x==null) return true;
		if (x instanceof UncertainNumerical) {
			// TODO handle [-1,1] != 0
		}
		return x.doubleValue()==0;
	}
	
	public Double getDelta() {
		return delta;
	}
	
	protected static final ICalculator calc = new Sample1Calculator(); 
	
	private static final long serialVersionUID = 1L;

	/**
	 * zero, but 'cos it's no value
	 */
	public static final Numerical NULL = new Numerical(0);
	
	private final Double value;
	
	/**
	 * for if breakdown-by-tag is being used
	 */
	Map<String,Double> value4tag;
	
	public String comment;
	
	/**
	 * Does not include leading "="
	 */
	public String excel;
	public String excelRef;
	
	/**
	 * treat % as a unit - but one that gets over-ridden by anything else
	 */
	private String unit;

	/**
	 * For comparing against another set of values
	 */
	private Double delta;

	/**
	 * HACK how many bits went into this number?
	 */
	public int cnt;

	// TODO remove the ,?
	public static Pattern number = Pattern.compile("-?(£|\\$)?([0-9]+[0-9,]+[0-9]{3}(:?\\.[0-9]+)?|[0-9][0-9,]*\\.[0-9]+|[0-9]+)(k|m|bn)?%?", Pattern.CASE_INSENSITIVE);
	
	/**
	 * @param s Accepts a range of things: £10, -£20, $5, 0.01, 1,000 5k, 5M, 1.5bn, 20%
	 */
	public Numerical(String s) {		
		Matcher m = number.matcher(s);
		if ( ! m.matches()) throw new IllegalArgumentException(s);
		Double v = Double.valueOf(m.group(2).replace(",", ""));
//		int gc = m.groupCount();
//		String g0 = m.group(0);
//		String g1 = m.group(1);
//		String g2 = m.group(2);
//		String g3 = m.group(3);
		String mk = m.group(4);
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

	/**
	 * 
	 * @param value If a Numerical, then copy value and unit but not comment, excel, or delta
	 */
	public Numerical(Number value) {
		this(value.doubleValue(), null); 
		if (value instanceof Numerical) {
			Numerical n = (Numerical) value;
			setUnit(n.getUnit());// ugly - pass down the unit
			if (n.value4tag != null) { // and tag breakdown
				this.value4tag = new ArrayMap(n.value4tag);
			}
		}
	}



	public Numerical(double value, String unit) {
		this.value = value;
		if ( ! MathUtils.isFinite(value) && getClass() == Numerical.class) { // divide by zero?
			String s = toString(); // for debug
		}
		this.unit = unit==null? null : unit.intern();		
	}

	@Override
	public String toString() {
		int sigFigs = 3;
		String sign = doubleValue() < 0? "-" : "";
		double v = Math.abs(doubleValue());
		if ("%".equals(unit)) {
			v *= 100;
		}
		// round + to string
		String num;
		if (v>1000000) {
			num = StrUtils.toNSigFigs(v/1000000, sigFigs)+"M";
		} else if (v>1000) {
			num = StrUtils.toNSigFigs(v/1000, sigFigs)+"k";
		} else if (Math.abs(v) < 0.00001) {
			num = "0";
		} else {
			num = StrUtils.toNSigFigs(v, sigFigs);
		}
		
		if (unit==null) {
			return sign+num;	
		}
		if ("%".equals(unit)) {
			return sign+num+'%';
		}
		return sign+unit+num;
	}


	/**
	 * Suitable for spreadsheets. Avoids human friendly abbreviations e.g. "£10k".
	 * Does not do commas 
	 * @param b 
	 */
	public String toExportString(boolean commas) {
		String sign = doubleValue() < 0? "-" : "";
		double v = Math.abs(doubleValue());
		if ("%".equals(unit)) {
			v *= 100;
		}
		// round (a bit) + to string
		String num;
		if (commas) {
			DecimalFormat f = new DecimalFormat("###,###.##");
			num = f.format(v);				
		} else {
			num = StrUtils.toNSigFigs(v, 8);
		}
		if (unit==null) {
			return sign+num;	
		}
		if ("%".equals(unit)) {
			return sign+num+'%';
		}
		if (num.matches("\\.\\d$")) {
			num += "0";
		}
		// No unit :( G-Sheets issue: "Function ADD parameter 1 expects number values. But '£232.91' is a text and cannot be coerced to a number."
		return sign+num; 
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
		return calc.timesScalar(this, b);
	}

	public Numerical minus(Numerical b) {
		return calc.minus(this, b);
	}

	@Override
	public Numerical plus(double b) {
		return calc.plus(this, new Numerical(b));
	}

	public void setDelta(Double d) {
		this.delta = d;
	}

	public Numerical setUnit(String unit) {
		this.unit = unit;
		return this;
	}

	/**
	 * Overwrites any existing tags on this Numerical.
	 * E.g. in this rule
	 * 	Blue Sales: (MixedSales - MixedSales#green) #blue
	 * The results should be all blue, and should not have a negative #green component.  
	 * @param tag
	 * @return this
	 */
	public Numerical setTag(String tag) {
//		if (value4tag==null)  new ArrayMap(tag, value);
//		value4tag.put(tag, value);
		value4tag = Collections.singletonMap(tag,  value);
		return this;
	}
	
	/**
	 * 
	 * @param tag
	 * @return the tagged value, or zero
	 */
	public Numerical getTagged(String tag) {
		Double tv = value4tag==null? null : value4tag.get(tag);
		if (tv==null) {
			return new Numerical(0).setUnit(unit); // no tag 'cos zero
		}
		Numerical n = new Numerical(tv);
		n.setUnit(getUnit());
		n.setTag(tag);
		return n;
	}
		

}

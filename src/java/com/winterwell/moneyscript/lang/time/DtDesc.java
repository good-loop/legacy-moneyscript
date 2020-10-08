package com.winterwell.moneyscript.lang.time;

import com.winterwell.moneyscript.lang.num.BasicFormula;
import com.winterwell.moneyscript.lang.num.Formula;
import com.winterwell.moneyscript.lang.num.Numerical;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;

/**
 * TODO support "2 or 3 months"
 * @author daniel
 *
 */
public class DtDesc {

	final Formula f;
	private TUnit unit;
		
	public DtDesc(Formula f, TUnit unit) {
		this.f = f;
		this.unit = unit;
	}

	public DtDesc(Dt dt) {
		this.f = new BasicFormula(new Numerical(dt.getValue()));
		this.unit = dt.getUnit();
	}

	public Dt calculate(Cell b) {
		Numerical n = f.calculate(b);
		return new Dt(n.doubleValue(), unit);
	}
	
	@Override
	public String toString() {
		return f+" "+unit;
	}

}

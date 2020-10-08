package com.winterwell.moneyscript.lang.bool;

import com.winterwell.moneyscript.lang.UncertainNumerical;
import com.winterwell.moneyscript.lang.num.Formula;
import com.winterwell.moneyscript.lang.num.Numerical;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Business.KPhase;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.utils.TodoException;

public abstract class Condition {

	public Condition() {
	}

	/**
	 * Is the condition true here? The name "contains" is to match with Filter
	 * @param cell
	 * @param b
	 * @return condition(cell) given b 
	 */
	public abstract boolean contains(Cell cell, Cell b);

}

class Comparison extends Condition {

	private Formula lhs;
	private Formula rhs;
	private String op;

	public Comparison(Formula lhs, String cmp, Formula rhs) {
		this.lhs = lhs;
		this.rhs = rhs;
		assert cmp.trim().equals(cmp) : cmp;
		this.op = cmp;
	}

	@Override
	public boolean contains(Cell cell, Cell b) {
		Numerical l = lhs.calculate(b);
		if (l==null) l = Numerical.NULL;
		Numerical r = rhs.calculate(b);
		if (r==null) r = Numerical.NULL; 			
		if (l instanceof UncertainNumerical || r instanceof UncertainNumerical) {				
			assert Business.get().getPhase() == KPhase.OUTPUT : this;
		}
		if ("<".equals(op)) {
			return l.doubleValue() < r.doubleValue();
		}
		if (">".equals(op)) {
			return l.doubleValue() > r.doubleValue();
		}
		if ("==".equals(op)) {
			return l.doubleValue() == r.doubleValue();
		}
		if ("<=".equals(op)) {
			return l.doubleValue() <= r.doubleValue();
		}
		if (">=".equals(op)) {
			return l.doubleValue() >= r.doubleValue();
		}
		throw new TodoException(op+" in "+toString());
	}

}

class Combi extends Condition {

	private Condition lhs;
	private Condition rhs;
	private String op;

	public Combi(Condition a, String o, Condition b) {
		this.lhs = a;
		this.op = o;
		this.rhs = b;
	}

	@Override
	public boolean contains(Cell cell, Cell b) {
		boolean l = lhs.contains(cell, b);
		if ("or".equals(op) || "||".equals(op)) {
			if (l) return l;
		} else {
			assert "and".equals(op);
			if (!l) return false;
		}
		return rhs.contains(cell, b);
	}

}


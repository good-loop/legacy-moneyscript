package com.winterwell.moneyscript.lang.bool;

import com.winterwell.moneyscript.output.Cell;

public abstract class Condition {

	public Condition() {
	}

	/**
	 * Is the condition true here? The name "contains" is to match with Filter
	 * @param cell
	 * @param context
	 * @return condition(cell) given context 
	 */
	public abstract boolean contains(Cell cell, Cell context);

	public static Not not(Condition base) {
		return new Not(base);
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


final class Not extends Condition {

	private Condition base;

	public Not(Condition base) {
		this.base = base;
	}

	@Override
	public boolean contains(Cell cell, Cell b) {
		boolean byes = base.contains(cell, b);
		return ! byes;
	}

	@Override
		public String toString() {
			return "Not["+base+"]";
		}
}


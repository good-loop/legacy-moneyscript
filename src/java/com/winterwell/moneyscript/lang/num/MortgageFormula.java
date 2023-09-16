package com.winterwell.moneyscript.lang.num;

import com.winterwell.moneyscript.lang.time.DtDesc;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.containers.Tree;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;

public class MortgageFormula extends Formula {


	private Formula capital;
	private Formula interestRate;
	private DtDesc period;

	@Override
	public Tree<Formula> asTree() {
		Tree t = new Tree(this);
		Tree tl = capital.asTree();
		Tree tr = interestRate.asTree();
		Tree tp = period.f.asTree();
		tl.setParent(t);
		tr.setParent(t);
		tp.setParent(t);
		return t;
	}
	
	public MortgageFormula(Formula capital, Formula interestRate, DtDesc period) {
		super("repay");
		this.capital = capital;
		this.interestRate = interestRate;
		this.period = period;
	}

	@Override
	public Numerical calculate(Cell b) {
		Numerical c = capital.calculate(b);
		Numerical _r = interestRate.calculate(b);
		// we must  have interest & capital
		if (_r == null) return null;
		if (c == null) return null;
		double r = _r.doubleValue();
		Dt totalMonths = period.calculate(b).convertTo(TUnit.MONTH);
		// +1 for zero indexing??
		double monthsLeft = totalMonths.getValue() - b.getColumn().index + 0;
		if (monthsLeft==0) {
			// hopefully capital is zero
			assert MathUtils.approx(c.doubleValue(), 0) : c;
			return c;
		}
		// from annual rate to monthly rate
		r = r/12;
		double rm = Math.pow(1+r, monthsLeft);
		double x = (r * rm) / (rm - 1);
		return c.times(x);
	}
	
//	=100000*((D3/12) * (1 + (D3/12))^300) / ((1 + (D3/12))^300 - 1)
}

package com.winterwell.moneyscript.lang.bool;

import java.util.Collections;
import java.util.Set;

import com.winterwell.moneyscript.lang.UncertainNumerical;
import com.winterwell.moneyscript.lang.cells.CurrentRow;
import com.winterwell.moneyscript.lang.num.BasicFormula;
import com.winterwell.moneyscript.lang.num.Formula;
import com.winterwell.moneyscript.lang.num.Numerical;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Business.KPhase;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.utils.TodoException;

public class Comparison extends Condition {

	private Formula lhs;
	private Formula rhs;
	private String op;

	@Override
	public String toString() {
		return "Comparison["+lhs+" "+op+" "+rhs+"]";
	}
	
	public Comparison(Formula lhs, String cmp, Formula rhs) {
		this.lhs = lhs;
		this.rhs = rhs;
		assert cmp.trim().equals(cmp) : cmp;
		this.op = cmp;
	}

	@Override
	public boolean contains(Cell cell, Cell context) {
		// HACK
		if ("in".equals(op)) {
			Set<String> lnames; 
			// fugly HACK
			if  (lhs instanceof BasicFormula && ((BasicFormula) lhs).getCellSetSelector() instanceof CurrentRow) {
				lnames = Collections.singleton(cell.row.getName());
			} else lnames = lhs.getRowNames(context); 
			Set<String> names = rhs.getRowNames(context);
			for (String lname : lnames) {
				if ( ! names.contains(lname)) return false;
			}
			return true;
		}
		
		Numerical l = lhs.calculate(cell); // NB: changed from `context` to `cell` Sept 2022 (a bugfix I think)
		if (l==null) l = Numerical.NULL;
		Numerical r = rhs.calculate(cell);
		if (r==null) r = Numerical.NULL; 			
		if (l instanceof UncertainNumerical || r instanceof UncertainNumerical) {
			if (l==Business.EVALUATING || r==Business.EVALUATING) {
				// HACK handle e.g. "Payrises from July 2022:\n		Staff if (this row at Jan 2022) > 0: + £1k per year 
				return false;
			}
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
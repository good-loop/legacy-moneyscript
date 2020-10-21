package com.winterwell.moneyscript.lang.bool;

import java.util.Collections;
import java.util.Set;

import com.winterwell.moneyscript.lang.UncertainNumerical;
import com.winterwell.moneyscript.lang.cells.CurrentRow;
import com.winterwell.moneyscript.lang.num.BasicFormula;
import com.winterwell.moneyscript.lang.num.Formula;
import com.winterwell.moneyscript.lang.num.Numerical;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Business.KPhase;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.containers.Containers;

public class Comparison extends Condition {

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
		// HACK
		if ("in".equals(op)) {
			Set<String> lnames; 
			// fugly HACK
			if  (lhs instanceof BasicFormula && ((BasicFormula) lhs).getCellSetSelector() instanceof CurrentRow) {
				lnames = Collections.singleton(cell.row.getName());
			} else lnames = lhs.getRowNames(); 
			Set<String> names = rhs.getRowNames();
			for (String lname : lnames) {
				if ( ! names.contains(lname)) return false;
			}
			return true;
		}
		
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
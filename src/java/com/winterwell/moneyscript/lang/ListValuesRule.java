package com.winterwell.moneyscript.lang;

import java.util.List;

import com.winterwell.moneyscript.lang.cells.CellSet;
import com.winterwell.moneyscript.lang.num.Formula;
import com.winterwell.moneyscript.lang.num.Numerical;
import com.winterwell.moneyscript.output.BusinessContext;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Col;

/**
 * A csv chunk, e.g. "10, 20, 30"
 * Use-case: e.g. to put actuals into a plan, Sales: 8, 19, 27
 * @testedby  ListValuesRuleTest}
 * @author daniel
 *
 */
public class ListValuesRule extends Rule {

	private List<Formula> values;

	public ListValuesRule(CellSet selector, List<Formula> values, String src, int indent) 
	{
		super(selector, null, src, indent);
		this.values = values;
	}
	
	@Override
	protected Numerical calculate2_formula(Cell b) {
		// Pick the nth formula from the start of the selected cells to supply the value
		Col start = getSelector().getStartColumn(b.getRow(), b);
		int i = b.getColumn().index - start.index;
		assert i >= 0 : b+" vs "+start;
		if (i >= values.size()) return null;
		Formula f = values.get(i);
		Numerical v = f.calculate(b);
		return v;
	}


}

package com.winterwell.moneyscript.lang;

import com.winterwell.moneyscript.lang.cells.CellSet;
import com.winterwell.moneyscript.lang.num.Numerical;
import com.winterwell.moneyscript.output.Cell;


public class DummyRule extends Rule {

	public DummyRule(CellSet sel, String src) {
		super(sel, null, src, 0);
	}

	@Override
	protected Numerical calculate2_formula(Cell b) {
		return null;
	}
	

}

package com.winterwell.moneyscript.output;

import java.util.HashMap;
import java.util.Map;

import com.winterwell.moneyscript.lang.num.Numerical;
import com.winterwell.utils.containers.Pair2;

/**
 * The spreadsheet values. Can be used to do monte-carlo stuff.
 * @author daniel
 *
 */
public class BusinessState {

	Map<Cell, Numerical> values = new HashMap<Cell, Numerical>();

	public Numerical get(Cell cell) {
		Numerical v = values.get(cell);
		return v;
	}

	public void set(Cell cell, Numerical v) {
		values.put(cell, v);		
	}

	@Override
	public String toString() {
		return "BusinessState[cell-values=" + values.size() + "]";
	}

}

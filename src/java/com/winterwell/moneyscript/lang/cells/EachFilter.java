/**
 * 
 */
package com.winterwell.moneyscript.lang.cells;

import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Col;
import com.winterwell.moneyscript.output.Row;
import com.winterwell.moneyscript.lang.time.DtDesc;
import com.winterwell.moneyscript.lang.time.TimeDesc;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;

/**
 * E.g. each year from month 3
 * @author daniel
 *
 */
public class EachFilter extends Filter {

	private DtDesc unit;
	
	/**
	 * This is where the each starts counting.
	 * It is NOT a 2nd filter.
	 */
	private TimeDesc from;

	public EachFilter(DtDesc each, TimeDesc from) {
		super(null);
		this.unit = each;
		this.from = from;
	}

	@Override
	public boolean contains(Cell cell, Cell bc) {
		Business b = bc.getBusiness();
		int i = cell.col.index;
		int start = 1;
		if (from != null) {
			start = from.getCol(bc).index;
		}
		Dt step = b.getTimeStep();
		Dt dt = unit.calculate(bc);
		double n = dt.divide(step);
		double a = (i-start)/n;
		if (a != Math.round(a)) return false;
		// do we include year 1? probably not
		if (a==0) return false;
		return true;
	}
}

package com.winterwell.moneyscript.lang.cells;

import java.util.Collection;

import com.winterwell.moneyscript.output.Cell;

public class FilteredCellSet extends CellSet {

	@Override
	public String toString() {
		return base+" "+filter;
	}
	
	public java.util.Set<String> getRowNames(Cell focus) {
		return base.getRowNames(focus);
	}
	
	CellSet base;
	Filter filter;

	public FilteredCellSet(CellSet base, Filter filter) {
		this.base = base;
		this.filter = filter;
	}

	@Override
	public boolean contains(Cell cell, Cell context) {
		boolean ok = base.contains(cell, context);
		if ( ! ok) return false;
		ok = filter.contains(cell, context);
		if ( ! ok) return false;
		return ok;
//		return getCells(context).contains(cell);
	}

	@Override
	public Collection<Cell> getCells(Cell bc, boolean wide) {
		Collection<Cell> cells = base.getCells(bc, wide); // should we set wide=true here??		
		return filter.filter(cells, bc);
	}

}

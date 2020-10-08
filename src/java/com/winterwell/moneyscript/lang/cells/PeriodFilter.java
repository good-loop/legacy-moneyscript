package com.winterwell.moneyscript.lang.cells;

import java.util.Collection;
import java.util.List;

import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.lang.time.DtDesc;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.time.Dt;

public class PeriodFilter extends Filter {

	private DtDesc dt;

	public PeriodFilter(DtDesc dt) {
		this.dt = dt;
	}

	@Override
	public boolean contains(Cell cell, Cell context) {
		// need to know the start of this cell-set!
		throw new IllegalStateException("use filter instead!");
	}
	
	@Override
	public Collection<Cell> filter(Collection<Cell> cells, Cell context) {
		if (cells.isEmpty()) return cells;
		List<Cell> _cells = Containers.getList(cells);
		Cell start = _cells.get(0);
		throw new TodoException(dt+" from "+start);
	}
	
}

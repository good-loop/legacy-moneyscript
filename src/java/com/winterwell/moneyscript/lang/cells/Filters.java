package com.winterwell.moneyscript.lang.cells;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Col;
import com.winterwell.moneyscript.lang.Lang;
import com.winterwell.moneyscript.output.Row;
import com.winterwell.moneyscript.lang.num.Numerical;
import com.winterwell.moneyscript.lang.time.TimeDesc;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.nlp.simpleparser.AST;
import com.winterwell.utils.Printer;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.containers.ArraySet;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.time.Dt;


/**
 * To/from point (inclusive of point)
 * @author daniel
 *
 */
class For extends Filter {

	private Dt dt;

	public For(Dt dt) {
		super(null);
		this.dirn = KDirn.RIGHT;
		assert dt != null;
	}

	@Override
	public Collection<Cell> filter(Collection<Cell> cells, Cell context) {
		int start = -1;
		Business b = Business.get();
		double n = dt.divide(b.getTimeStep());
		List<Cell> cells2 = new ArrayList<Cell>();
		for (Cell cell : cells) {
			if (start==-1) start = cell.col.index;
			else {
				int i = cell.col.index - start;
				if (i >= n) break;
			}
			cells2.add(cell);
		}
		return cells2;
	}		
	
	@Override
	public String toString() {
		return "for "+dt;
	}

	@Override
	public boolean contains(Cell cell, Cell b) {
		throw new TodoException(toString());
	}
}

	
class ByUnit extends Filter {

		private String unit;

		public ByUnit(String unit) {
			super(null);
			assert unit != null;
			this.unit = unit;
		}
		
		@Override
		public boolean contains(Cell cell, Cell context) {
			Business b = Business.get();
			Numerical v = b.getCellValue(cell);
			if (v==null) return false;
			return unit.equals(v.getUnit());
		}

	}

/**
 * Chain filters together
 * @author daniel
 *
 */
class Chain extends Filter {
		
	private List<Filter> filters;

	@Override
	public String toString() {
		return Printer.toString(filters, " && ");
	}
	
	public Chain(List<Filter> filters) {
		super(null);
		this.filters = filters;
		for (Filter f : filters) {
			if (dirn==null) dirn = f.dirn; 
		}
	}

	@Override
	public Collection<Cell> filter(Collection<Cell> cells, Cell context) {
		for (Filter f : filters) {
			cells = f.filter(cells, context);
		}
		return cells;
	}

	@Override
	public boolean contains(Cell cell, Cell context) {
		// Hm... not always correct!
		for (Filter f : filters) {
			if ( ! f.contains(cell, context)) {
				return false;
			}
		}
		return true;
	}		
}
	

class Above extends Filter {

	public Above() {
		super(null);
		dirn = KDirn.UP;
	}
	
	@Override
	public boolean contains(Cell cell, Cell context) {
		Col c = context.getColumn();
		if (c.index != cell.col.index) return false;
		Row r = context.getRow();
		int herei = r.getIndex();
		int rowi = cell.row.getIndex();
		return rowi < herei;
	}
	
}
	




package com.winterwell.moneyscript.lang.cells;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.winterwell.moneyscript.lang.num.Numerical;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Col;
import com.winterwell.moneyscript.output.Row;
import com.winterwell.utils.TodoException;
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
	




package com.winterwell.moneyscript.lang.cells;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.winterwell.moneyscript.lang.bool.Condition;
import com.winterwell.moneyscript.lang.time.LangTime;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Col;
import com.winterwell.moneyscript.output.Row;
import com.winterwell.utils.TodoException;

/**
 * 
 * @testedby ConditionalFilterTest
 * @author daniel
 *
 */
public class ConditionalFilter extends Filter {
	
	private Condition cond;

	public ConditionalFilter(String op, Condition tst) {
		super(null);
		this.op = op;
		assert "if".equals(op) || LangTime.from.equals(op) || "to".equals(op) : "TODO "+op;
		this.cond = tst;
		dirn = KDirn.LEFT; // ??
	}
	
	@Override
	public boolean contains(Cell cell, Cell context) {
		if ("if".equals(op)) {
			return cond.contains(cell, context);
		}
		if (LangTime.from.equals(op)) {
			Cell start = getStart(cell.row, context);
			if (start==null) return false;
			return cell.col.index >= start.col.index;
		}
		if ("to".equals(op)) {
			Cell start = getStart(cell.row, context);
			if (start==null) return false;
			return cell.col.index <= start.col.index;
		}
		// TODO Auto-generated method stub
		throw new TodoException(toString());
	}
	
	/**
	 * @param row
	 * @param b
	 * @return first cell in this row that meets the condition
	 */
	private Cell getStart(Row row, Cell b) {
		List<Col> cols = b.getBusiness().getColumns();
		for (Col col : cols) {
			Cell cell = new Cell(row, col);
			if (cond.contains(cell, b)) {
				return cell;
			}
		}
		return null;
	}

	@Override
	public Collection<Cell> filter(Collection<Cell> cells, Cell context) {
		if ("if".equals(op)) {
			List<Cell> cells3 = new ArrayList<Cell>(cells.size());
			for (Cell cell : cells) {
				if ( ! cond.contains(cell, context)) continue;
				cells3.add(cell);
			}
			return cells3;
		}		
		if (LangTime.from.equals(op) || "to".equals(op)) {
			// avoid lookinh up start all the time
			List<Cell> cells3 = new ArrayList<Cell>(cells.size());
			Cell start = null;
			for (Cell cell : cells) {
				if (start==null || start.row != cell.row) {
					start = getStart(cell.row, context);
					if (start==null) continue;
				}				
				if (cell.col.index == start.col.index) {
					cells3.add(cell);
					continue;
				}
				if (("from".equals(op) && cell.col.index > start.col.index)
					|| ("to".equals(op) && cell.col.index < start.col.index)) {
					cells3.add(cell);
					continue;
				}
			}
			return cells3;
		}
		
		// TODO Auto-generated method stub
		return super.filter(cells, context);
	}
	

}

package com.winterwell.moneyscript.lang.cells;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.winterwell.moneyscript.output.Cell;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.containers.ArraySet;

/**
 * e.g. "split by Staff"
 * @author daniel
 *
 */
public class RowSplitCellSet extends CellSet {
	
	CellSet splitBy;
	private CellSet base;	

	public RowSplitCellSet(CellSet base, CellSet splitBy, String src) {
		super(src);
		this.base = base;
		this.splitBy = splitBy;
	}

	@Override
	public boolean contains(Cell cell, Cell context) {
		Set<String> rowNames = getRowNames(context);
		if (rowNames.contains(cell.row.getName())) {
			return true;
		}
		return false;
	}
	
	@Override
	public Set<String> getRowNames(Cell focus) {
		String grpRow = getSrc();
		Set<String> baseRows = splitBy.getRowNames(focus);
		ArraySet set = new ArraySet();
		set.add(grpRow);
		for(String baseRow : baseRows) {
			if (baseRow.equals(splitBy.getSrc())) {
				continue;
			}
			String splitRow = base.getSrc()+" for "+baseRow;
			set.add(splitRow);
		}
		return set;
	}

	@Override
	public Collection<Cell> getCells(Cell bc, boolean wide) {
		if (true) throw new TodoException();
		Collection<Cell> ac = splitBy.getCells(bc, wide);
		HashSet<Cell> cells = new HashSet<Cell>(ac);
		return cells;
	}

}

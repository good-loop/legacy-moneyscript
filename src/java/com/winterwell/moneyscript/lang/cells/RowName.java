package com.winterwell.moneyscript.lang.cells;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.BusinessContext;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Col;
import com.winterwell.moneyscript.output.Row;
import com.winterwell.utils.containers.ArraySet;
import com.winterwell.utils.containers.Containers;

public final class RowName extends CellSet {
	private final String rowName;
	
	public RowName(String rowName) {
		super(rowName);
		assert rowName != null;
//		assert LangCellSet.rowName.parse(rowName) != null : rowName;
		this.rowName = rowName;
	}
	
	public String getRowName() {
		return rowName;
	}
	
	@Override
	public Set<String> getRowNames(Cell focus) {
		Row row = Cell.getBusiness().getRow(rowName);
		if (row != null && row.isGroup()) {
			List<Row> rows = row.flatten();			
			List<String> names = Containers.apply(rows, r -> r.getName());
			return new ArraySet(names);
		}
		return Collections.singleton(rowName);
	}
	
	@Override
	public String toString() {
		return rowName;
	}
	@Override
	public boolean contains(Cell cell, Cell context) {
		Row myRow = getRow(context);
		return contains2(myRow, cell);
	}
	
	private Row getRow(Cell ignored) {
		Business b = BusinessContext.getBusiness();
		Row r = b.getRow(rowName);
		return r;
	}

	/**
	 * 
	 * @param myRow
	 * @param cell
	 * @return true if cell is in myRow or its children
	 */
	private boolean contains2(Row myRow, Cell cell) {
		if (cell.row == myRow) return true;
		if ( ! myRow.isGroup()) return false;
		// NB: Profiling showed this as a bottleneck, Jan 2022
		if (_subRows==null) {
			_subRows = new HashSet();
			initSubRows(myRow);
		}
		boolean in = _subRows.contains(cell.row.getName());
		return in;
	}		
	
	private void initSubRows(Row myRow) {		
		for(Row kRow : myRow.getChildren()) {
			_subRows.add(kRow.getName());
			initSubRows(kRow);
		}			
	}

	transient Set<String> _subRows;
	
	
	
	
	/**
	 * the equivalent cell in this row. e.g. for rows A, B    
	 * then RowName("A").getCells(context: {row:B, col:1}) = {row:A, col:1}
	 * 
	 *  Use-case: e.g. formulae like "B: 2 * A"
	 *  @return HACK: null if the row-name is invalid
	 */
	@Override
	public Collection<Cell> getCells(Cell bc, boolean wide) {
		Row row = getRow(bc);
		if (row==null) {
			// HACK 
			return null;			
		}
		assert row != null : rowName+" in "+bc;
		if (wide) {
			return row.getCells();
		}
		Col col = bc.getColumn();
		Cell cell = new Cell(row, col);
		return Collections.singletonList(cell);
	}
}


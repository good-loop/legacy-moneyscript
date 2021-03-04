package com.winterwell.moneyscript.lang.cells;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.winterwell.moneyscript.output.Cell;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.containers.ArraySet;

public class RowListCellSet extends CellSet {
	private final List<String> rowName;
	
	public RowListCellSet(List<String> rowName, String src) {
		super(src);
		assert rowName != null;
//		assert LangCellSet.rowName.parse(rowName) != null : rowName;
		this.rowName = rowName;
	}
	
	@Override
	public Set<String> getRowNames(Cell focus) {
		return new ArraySet(rowName);
	}
	
	@Override
	public String toString() {
		return rowName.toString();
	}
	@Override
	public boolean contains(Cell cell, Cell context) {
		throw new TodoException();
//		Row myRow = getRow(context);
//		return contains2(myRow, cell);
	}
	
//	private Row getRow(Cell ignored) {
//		return BusinessContext.getBusiness().getRow(rowName);
//	}

//	private boolean contains2(Row myRow, Cell cell) {
//		if (cell.row == myRow) return true;
//		if ( ! myRow.isGroup()) return false;
//		for(Row kRow : myRow.getChildren()) {
//			boolean yes = contains2(kRow, cell);
//			if (yes) return true;
//		}
//		return false;
//	}		
	
	/**
	 * the equivalent cell in this row. e.g. for rows A, B    
	 * then RowName("A").getCells(context: {row:B, col:1}) = {row:A, col:1}
	 * 
	 *  Use-case: e.g. formulae like "B: 2 * A"
	 */
	@Override
	public Collection<Cell> getCells(Cell bc, boolean wide) {
		throw new TodoException();
//		Row row = getRow(bc);
//		assert row != null : rowName+" in "+bc;
//		if (wide) {
//			return row.getCells();
//		}
//		Col col = bc.getColumn();
//		Cell cell = new Cell(row, col);
//		return Collections.singletonList(cell);
	}

}

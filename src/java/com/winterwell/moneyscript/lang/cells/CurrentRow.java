package com.winterwell.moneyscript.lang.cells;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Row;

public final class CurrentRow extends CellSet {

	public CurrentRow(String src) {
		super(src);
	}

	@Override
	public String toString() {
		return "CurrentRow";
	}
	
	@Override
	public boolean contains(Cell cell, Cell context) {
		Row r = context.getRow();
		return cell.row==r;
	}		
	
	@Override
	public Set<String> getRowNames(Cell focus) {
		throw new UnsupportedOperationException("CurrentRow is too flexible for getRowNames()");
	}

	@Override
	public Collection<Cell> getCells(Cell b, boolean wide) {
		if (wide) {
			Row row = b.getRow();
			return row.getCells();
		}
		return Collections.singleton(b);
	}		
	
}

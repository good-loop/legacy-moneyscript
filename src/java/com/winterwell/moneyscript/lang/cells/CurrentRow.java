package com.winterwell.moneyscript.lang.cells;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Row;

public class CurrentRow extends CellSet {

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
	public Set<String> getRowNames() {
		throw new UnsupportedOperationException(getClass().getName());
	}

	@Override
	public Collection<Cell> getCells(Cell b, boolean wide) {
		if (wide) {
			throw new UnsupportedOperationException("wide not supported for "+this);
		}
		return Collections.singleton(b);
	}		
	
}

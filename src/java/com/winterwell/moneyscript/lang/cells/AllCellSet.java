package com.winterwell.moneyscript.lang.cells;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Row;

/**
 * Selects all rows & columns!
 * @author daniel
 *
 */
public class AllCellSet extends CellSet {

	@Override
	public boolean contains(Cell cell, Cell context) {
		return true;
	}

	@Override
	public String toString() {
		return "AllCellSet";
	}

	@Override
	public Collection<Cell> getCells(Cell bc, boolean wide) {
		List<Row> rows = bc.getBusiness().getRows();
		Collection<Cell> cells = new HashSet<Cell>();
		for (Row row : rows) {
			cells.addAll(row.getCells());
		}
		return cells;
	}

	@Override
	public Set<String> getRowNames() {
		Business b = Business.get();
		List<Row> rows = b.getRows();
		Set<String> rns = new HashSet<String>();
		for (Row row : rows) {
			rns.add(row.getName());
		}
		return rns;
	}

}

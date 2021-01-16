package com.winterwell.moneyscript.output;

import java.util.HashMap;
import java.util.Map;

import com.winterwell.moneyscript.lang.num.Numerical;

/**
 * The spreadsheet values. Can be used to do monte-carlo stuff.
 * @author daniel
 *
 */
public final class BusinessState {

	private final Numerical[][] values;
	
	public BusinessState(Business b) {
		int nrows = b.getRows().size();
		int ncols = b.getColumns()==null? 37 : b.getColumns().size();
		this.values = new Numerical[nrows][ncols+1]; // NB: 1 indexed
	}

	public Numerical get(Cell cell) {
		Numerical v = values[cell.row.getIndex()][cell.col.index];
		return v;
	}

	public void set(Cell cell, Numerical v) {
		values[cell.row.getIndex()][cell.col.index] = v;
	}

	@Override
	public String toString() {
		return "BusinessState";
	}

}

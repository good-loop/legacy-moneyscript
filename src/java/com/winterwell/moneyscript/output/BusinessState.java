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
		this(b.getRows().size(), b.getColumns().size());
	}
	
	BusinessState(int nrows, int ncols) {
		assert nrows > 0 && ncols > 0;
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

	public static BusinessState testBS(Business business) {
		BusinessState bs = new BusinessState(10,24);
		return bs;
	}

}

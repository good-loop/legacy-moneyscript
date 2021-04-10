package com.winterwell.moneyscript.output;

import java.util.Arrays;

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
		assert nrows > 0 && ncols > 0 : "No rows or cols?! rows:"+nrows+" cols:"+ncols;
		this.values = new Numerical[nrows][ncols+1]; // NB: 1 indexed
	}

	public Numerical get(Cell cell) {
		Numerical v = values[cell.row.getIndex()][cell.col.index];
		return v;
	}

	public void set(Cell cell, Numerical v) {
		Numerical[] r = values[cell.row.getIndex()];
		r[cell.col.index] = v;
	}

	@Override
	public String toString() {
		return "BusinessState";
	}

	public static BusinessState testBS(Business business) {
		BusinessState bs = new BusinessState(10,24);
		return bs;
	}

	public void resize(int nrows, int ncols) {
		Numerical[][] newValues = new Numerical[nrows][ncols+1]; // NB: 1 indexed on cols
		int nr = Math.min(nrows, values.length);
		for (int ri = 0; ri < nr; ri++) {
			Numerical[] oldvri = values[ri];
			if (oldvri==null) continue;
			if (oldvri.length==ncols) {
				newValues[ri] = oldvri;
				continue;
			}
			newValues[ri] = Arrays.copyOf(oldvri, ncols);
		}
	}

}

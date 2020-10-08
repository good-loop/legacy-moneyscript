package com.winterwell.moneyscript.output;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;
/**
 * A column in the output sheet
 * @author daniel
 *
 */
public final class Col {

	/**
	 * Can also be interpreted as "never"
	 */
	public static final Col THE_INDEFINITE_FUTURE = new Col(Integer.MAX_VALUE/2);
	public static final Col THE_PAST = new Col(-1, true);
	
	/**
	 * 1 indexed! Because MoneyScript column-references are 1-indexed
	 * @param m
	 */
	public Col(int m) {
		this(m, true);
		assert m > 0;		
	}
	
	private Col(int m, boolean unsafe) {
		index = m;
	}
	
	@Override
	public String toString() {
		return "Col["
				+(index==THE_INDEFINITE_FUTURE.index? "future" : (index+" "+getTimeDesc()))
				+"]";
	}

	public int index;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + index;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Col other = (Col) obj;
		if (index != other.index)
			return false;
		return true;
	}

	public Collection<Cell> getCells() {
		Business b = Business.get();
		List<Row> rows = b.getRows();
		List<Cell> cells = new ArrayList<Cell>();
		for (Row row : rows) {
			cells.add(new Cell(row, this));
		}
		return cells;
	}

	public String getTimeDesc() {
		Business b = Business.get();
		Time start = b.settings.getStart();
		Dt step = b.settings.timeStep;
		if (start==null) {
			assert step.equals(TUnit.MONTH.dt) : step;
			if (index > 12) {
				int yr = 1 + (index-1)/12;
				int m = 1 + (index-1) % 12;
				return "year "+yr+" month "+m;
			}
			return "month "+index;
		}		
		Time t = getTime();
		return t.format("MMM yyyy");
	}

	public Time getTime() {
		Business b = Business.get();
		Time start = b.settings.getStart();
		Dt step = b.settings.timeStep;
		if (start==null) {
			start = new Time();
		}		
		Time t = start.plus(step.multiply(index-1));
		return t;
	}
	
}

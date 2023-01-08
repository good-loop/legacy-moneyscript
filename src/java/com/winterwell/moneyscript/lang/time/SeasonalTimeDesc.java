package com.winterwell.moneyscript.lang.time;

import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Col;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.time.Time;

/**
 * e.g. Q1 (of each year) or December (of each year)
 * Status: not used yet
 * @author daniel
 *
 */
public class SeasonalTimeDesc extends TimeDesc {

	private Time start;
	private Time end;

	public SeasonalTimeDesc(Time start, Time end, String parsed) {
		super(parsed);
		this.start = start;
		this.end = end;
	}

	@Override
	public Col getCol(Cell context) {
		final Col focusCol = context==null? null : context.getColumn();
		if (focusCol==null) {
			return Col.THE_INDEFINITE_FUTURE;
		}
		Time year = focusCol.getTime();
		Time s = new Time(year.getYear(), start.getMonth(), start.getDayOfMonth());
//		Time e = new Time(year.getYear(), end.getMonth(), end.getDayOfMonth());
		Col col = Business.get().getColForTime(s);
		return col;
	}
	
	@Override
	public Time getTime() {
		throw new TodoException();
	}

	@Override
	public String toString() {
		return "SeasonalTimeDesc [start=" + start + ", end=" + end + "]";
	}
	

}

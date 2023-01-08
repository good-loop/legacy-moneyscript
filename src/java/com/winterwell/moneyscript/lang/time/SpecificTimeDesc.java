package com.winterwell.moneyscript.lang.time;

import com.winterwell.moneyscript.lang.Settings;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Col;
import com.winterwell.utils.time.Time;

/**
 * An actual date in the calendar.
 * @author daniel
 *
 */
public class SpecificTimeDesc extends TimeDesc {

	private final Time time;
	private int months;
	
	public SpecificTimeDesc(Time time, String parsed) {
		super(parsed);
		this.time = time;
		assert time != null;
	}
	

	/**
	 * @return The (first) column for this time desc.
	 * Can be Col.THE_INDEFINITE_FUTURE or Col.THE_PAST. never null.
	 */
	public Col getCol(Cell context) {
		Col col = context.getBusiness().getColForTime(time);
		if (col!=null) return col;
		Settings bs = context.getBusiness().getSettings();
		if (time.isBefore(bs.getStart())) {
			return Col.THE_PAST;
		}
		return Col.THE_INDEFINITE_FUTURE;
	}
	
	@Override
	public Time getTime() {
		return time;
	}


	/**
	 * Normally unset (0). Use for e.g. quarters
	 * @param dt
	 */
	public void setMonths(int months) {
		this.months = months;
	}
	
	public int getMonths() {
		return months;
	}
}

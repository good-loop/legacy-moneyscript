package com.winterwell.moneyscript.lang.time;

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
		return context.getBusiness().getColForTime(time);
	}
	
	@Override
	public Time getTime() {
		return time;
	}
}

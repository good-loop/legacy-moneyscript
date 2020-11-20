package com.winterwell.moneyscript.lang.time;

import com.winterwell.utils.TodoException;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;

public class RelativeTimeDesc extends TimeDesc {

	public RelativeTimeDesc(TUnit unit, double n) {
		super(unit, n);
	}

	public RelativeTimeDesc(String string) {
		super(string);
	}

	public RelativeTimeDesc(DtDesc _dt, String from, TimeDesc td) {
		super(_dt,from,td);
	}

	@Override
	public Time getTime() {
		throw new TodoException(this);
	}

}

package com.winterwell.moneyscript.lang.time;

import com.winterwell.moneyscript.lang.bool.Condition;
import com.winterwell.moneyscript.lang.cells.Filter;
import com.winterwell.utils.time.Time;

public class ConditionalTimeDesc extends TimeDesc {

	private Condition cond;

	public ConditionalTimeDesc(String parsed, Condition cond) {
		super(parsed);
		this.cond = cond;
	}

	@Override
	public Time getTime() {
		throw new UnsupportedOperationException(toString());
	}

}

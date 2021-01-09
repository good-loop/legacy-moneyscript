package com.winterwell.moneyscript.lang.time;

import com.winterwell.moneyscript.lang.cells.Filter;
import com.winterwell.utils.time.Time;

public class ConditionalTimeDesc extends TimeDesc {

	private Filter filter;

	public ConditionalTimeDesc(String parsed, Filter f) {
		super(parsed);
		this.filter = f;
	}

	@Override
	public Time getTime() {
		throw new UnsupportedOperationException(toString());
	}

}

package com.winterwell.moneyscript.lang.time;

import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Col;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.time.Time;

/**
 * @deprecated WIP
 * @author daniel
 *
 */
public class ColumnIndexTimeDesc extends TimeDesc {
	
	private int index;

	public ColumnIndexTimeDesc(int index) {
		super("month "+index);
		this.index = index;
	}

	@Override
	public Time getTime() {
		Business b = Business.get();
		Col col = b.getColumns().get(index);
		return col.getTime();
	}

}

package com.winterwell.moneyscript.lang;

import com.winterwell.moneyscript.lang.cells.CellSet;
import com.winterwell.moneyscript.lang.cells.Filter;

public class MetaRule extends Rule {

	public String meta;

	public MetaRule(CellSet selector, String meta, String src) {
		super(selector, null, src, 0);
		this.meta = meta;
	}

}

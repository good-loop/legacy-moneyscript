package com.winterwell.moneyscript.lang.cells;

import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Row;

/**
 * e.g. "Region=UK"
 * @author daniel
 *
 */
public class SetVariable {

	public final String value;
	public final String var;

	public SetVariable(String var, String value) {
		this.var = var;
		this.value = value;
	}

	@Override
	public String toString() {
		return var+"="+value;
	}

	/**
	 * true if biz currently has var=value set by e.g. [Product in ProductMix]
	 * @param biz
	 * @return
	 */
	public boolean isTrue(Business biz) {
		Row row = biz.getRow(var);
		if (row==null) {
			return false;
		}
		if (row.getName().equals(value)) {
			return true;
		}
		return false;
	}

}

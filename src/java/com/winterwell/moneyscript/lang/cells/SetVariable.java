package com.winterwell.moneyscript.lang.cells;

/**
 * e.g. "Region=UK"
 * @author daniel
 *
 */
public class SetVariable {

	final Object value;
	final String var;

	public SetVariable(String var, Object value) {
		this.var = var;
		this.value = value;
	}

	@Override
	public String toString() {
		return var+"="+value;
	}

}

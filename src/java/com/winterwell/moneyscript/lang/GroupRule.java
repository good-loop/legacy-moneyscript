package com.winterwell.moneyscript.lang;

import com.winterwell.moneyscript.lang.cells.CellSet;



/**
 * Rule for creating a group. Carries no formula or other info
 * @author daniel
 *
 */
public class GroupRule extends Rule {

	/**
	 * If true means skip in calculations
	 */
	boolean na;
	
	/**
	 * If true means skip in calculations
	 */
	public boolean isNA() {
		return na;
	}

	public GroupRule(CellSet row, int indent) {
		super(row, null, row+":", indent);
		assert getSelector() != null : this;
	}

}

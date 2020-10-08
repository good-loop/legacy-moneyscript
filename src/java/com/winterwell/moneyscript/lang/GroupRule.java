package com.winterwell.moneyscript.lang;

import com.winterwell.moneyscript.lang.cells.RowName;
import com.winterwell.moneyscript.lang.cells.Scenario;



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

	public GroupRule(RowName row, int indent) {
		super(row, null, row+":", indent);
	}

	public GroupRule(Scenario scenario, int indent) {
		super(null, null, "scenario "+scenario+":", indent);
	}

}

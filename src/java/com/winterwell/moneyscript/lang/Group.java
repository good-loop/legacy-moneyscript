package com.winterwell.moneyscript.lang;

import com.winterwell.moneyscript.lang.cells.Filter;
import com.winterwell.moneyscript.lang.cells.Scenario;
import com.winterwell.moneyscript.output.Row;
import com.winterwell.utils.Utils;

/**
 * marker for grouping rules
 * @author daniel
 *
 */
final class Group {
	
	@Override
	public String toString() {		
		return Utils.toString(this);
	}
	public Group(Row row, int indent) {
		this.indent = indent;
		byRow = row;
		byScenario = null;
		byFilter = null;
	}
	public Group(Scenario row, int indent) {
		this.indent = indent;
		byRow = null;
		byScenario = row;
		byFilter = null;
	}
	public Group(Filter f, int indent) {
		this.indent = indent;
		byRow = null;
		byScenario = null;
		byFilter = f;
	}
	
	/**
	 * No group
	 */
	public Group() {
		indent = 0;
		byRow = null; 
		byScenario = null; 
		byFilter = null;
	}

	final Row byRow;
	final Scenario byScenario;
	final Filter byFilter;
	final int indent;
	Group parent;
	GroupRule rule;
	
	public void setParent(Group parent) {
		this.parent = parent;
	}
}
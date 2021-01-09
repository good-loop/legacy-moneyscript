package com.winterwell.moneyscript.lang.cells;

import com.winterwell.utils.AString;

/**
 * @author daniel
 *
 */
public class Scenario extends AString {

	/**
	 * HACK track the rules for preenting info to the user
	 */
	public String ruleText = "";
	
	public Scenario(CharSequence name) {
		super(name);
	}

}

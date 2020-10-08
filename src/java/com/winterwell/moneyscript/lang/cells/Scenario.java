package com.winterwell.moneyscript.lang.cells;

/**
 * @author daniel
 *
 */
public class Scenario {

	@Override
	public String toString() {	
		return name;
	}
	final String name;
	
	public Scenario(String name) {
		this.name = name;
	}
}

package com.winterwell.moneyscript.lang;

import com.winterwell.moneyscript.lang.cells.CellSet;


public class StyleRule extends Rule {

	private String css;

	public StyleRule(CellSet selector, String css, String src, int indent) {
		super(selector, null, src, indent);
		this.css = css;
	}
	
	public String getCSS() {
		return css;
	}

}

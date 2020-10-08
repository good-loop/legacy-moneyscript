package com.winterwell.calstat;

import com.winterwell.bob.wwjobs.BuildWinterwellProject;

public class BuildMoneyscript extends BuildWinterwellProject {

	public BuildMoneyscript() {
		super("moneyscript");
		setMainClass("com.winterwell.moneyscript.webapp.MoneyScriptMain");
//		setMakeFatJar(true); // odd classpath issues -- this is one (not ideal but simple) way to fix them
	}

}

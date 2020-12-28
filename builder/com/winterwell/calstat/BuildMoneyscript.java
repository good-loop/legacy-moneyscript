package com.winterwell.calstat;

import java.util.List;

import com.winterwell.bob.BuildTask;
import com.winterwell.bob.tasks.MavenDependencyTask;
import com.winterwell.bob.wwjobs.BuildWinterwellProject;

public class BuildMoneyscript extends BuildWinterwellProject {

	public BuildMoneyscript() {
		super("moneyscript");
		setMainClass("com.winterwell.moneyscript.webapp.MoneyScriptMain");
//		setMakeFatJar(true); // odd classpath issues -- this is one (not ideal but simple) way to fix them
	}

	@Override
	public List<BuildTask> getDependencies() {
		List<BuildTask> bts = super.getDependencies();
		
		MavenDependencyTask mdt = new MavenDependencyTask();
		mdt.addDependency("com.google.api-client:google-api-client:1.30.4");
		mdt.addDependency("com.google.oauth-client:google-oauth-client-jetty:1.30.6");
		mdt.addDependency("com.google.apis:google-api-services-sheets:v4-rev581-1.25.0");
		mdt.setIncSrc(true);
		bts.add(mdt);
		
		return bts;
	}
	
}

package com.winterwell.calstat;

import java.util.List;

import com.winterwell.bob.BuildTask;
import com.winterwell.bob.tasks.MavenDependencyTask;
import com.winterwell.bob.wwjobs.BuildWinterwellProject;

public class BuildMoneyscript extends BuildWinterwellProject {

	public BuildMoneyscript() {
		super("moneyscript");
		setVersion("1.0.0"); // Jan 2022
		setMainClass("com.winterwell.moneyscript.webapp.MoneyScriptMain");
//		setMakeFatJar(true); // odd classpath issues -- this is one (not ideal but simple) way to fix them
	}

	@Override
	public List<BuildTask> getDependencies() {
		List<BuildTask> bts = super.getDependencies();
		
		MavenDependencyTask mdt = new MavenDependencyTask();
		
		// ?? explicitly list jackson to avoid a VerifyError jar-versioning bug
		// -- doesn't work :( For some reason the older jars are winning
//		mdt.addDependency("com.google.api-client:google-api-client:1.30.4");
		// https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-core
		mdt.addDependency("com.fasterxml.jackson.core", "jackson-core", "2.12.2");
		mdt.addDependency("com.fasterxml.jackson.core", "jackson-databind", "2.12.2");
		mdt.addDependency("com.fasterxml.jackson.core", "jackson-annotations", "2.12.2");

//		mdt.addDependency("com.google.api-client:google-api-client:1.31.4");
//		mdt.addDependency("com.google.oauth-client:google-oauth-client-jetty:1.31.5");
//		mdt.addDependency("com.google.apis:google-api-services-sheets:v4-rev612-1.25.0");
		// https://mvnrepository.com/artifact/com.google.apis/google-api-services-sheets

		mdt.setIncSrc(true);
		bts.add(mdt);
		
		return bts;
	}
	
}

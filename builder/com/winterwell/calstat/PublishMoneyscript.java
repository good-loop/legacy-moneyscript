package com.winterwell.calstat;

import java.util.List;

import com.winterwell.bob.BuildTask;
import com.winterwell.web.app.build.KPubType;
import com.winterwell.web.app.build.PublishProjectTask;

public class PublishMoneyscript extends PublishProjectTask {
	
	public PublishMoneyscript() throws Exception {
		super("moneyscript", "moneyscript");
		typeOfPublish = KPubType.production;
	}
	
	@Override
	public List<BuildTask> getDependencies() {
		List<BuildTask> deps = super.getDependencies();
		deps.add(new BuildMoneyscript());
		return deps;
	}

}

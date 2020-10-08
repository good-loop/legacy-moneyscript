package com.winterwell.moneyscript.webapp;

import java.io.File;
import java.net.URI;
import java.util.Map;

import com.winterwell.data.KStatus;
import com.winterwell.datalog.DataLog;
import com.winterwell.moneyscript.data.PlanDoc;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.web.app.AMain;
import com.winterwell.web.app.AppUtils;
import com.winterwell.web.app.JettyLauncher;
import com.winterwell.web.app.MasterServlet;

public class MoneyScriptMain extends AMain<MoneyScriptConfig> {

	public MoneyScriptMain() {
		super("moneyscript", MoneyScriptConfig.class);
	}
	
	public static void main(String[] args) throws Exception {
		MoneyScriptMain mm = new MoneyScriptMain();
		mm.doMain(args);
	}

	@Override
	protected void addJettyServlets(JettyLauncher jl) {
		super.addJettyServlets(jl);
		MasterServlet ms = jl.addMasterServlet();	
		ms.addServlet("/plandoc", PlanDocServlet.class);
		ms.addServlet("/money", MoneyServlet.class);
	}
	
	@Override
	protected void init2(MoneyScriptConfig config) {
		super.init2(config);
		// datalog
		DataLog.init();
		// YA
		init3_youAgain();
		init3_emailer();
		// ES
		init3_gson();
		init3_ES();
		Class[] dbclasses = new Class[] {PlanDoc.class};
		AppUtils.initESIndices(KStatus.main(), dbclasses);
		Map<Class, Map> mappingFromClass = new ArrayMap();
		AppUtils.initESMappings(KStatus.main(), 
				dbclasses, mappingFromClass);
	}
}

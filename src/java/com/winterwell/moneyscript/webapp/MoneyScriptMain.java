package com.winterwell.moneyscript.webapp;

import java.util.Map;

import com.goodloop.gsheets.GSheetsClient;
import com.winterwell.data.KStatus;
import com.winterwell.datalog.DataLog;
import com.winterwell.es.XIdTypeAdapter;
import com.winterwell.gson.Gson;
import com.winterwell.gson.GsonBuilder;
import com.winterwell.gson.KLoopPolicy;
import com.winterwell.gson.StandardAdapters;
import com.winterwell.moneyscript.data.PlanDoc;
import com.winterwell.utils.AString;
import com.winterwell.utils.Dep;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Slice;
import com.winterwell.utils.time.Time;
import com.winterwell.web.app.AMain;
import com.winterwell.web.app.AppUtils;
import com.winterwell.web.app.JettyLauncher;
import com.winterwell.web.app.MasterServlet;
import com.winterwell.web.data.XId;

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
		ms.addServlet("/gsheet", GSheetServlet.class);
	}
	
	/**
	 * Add in Slice handling
	 */
	protected @Override
	GsonBuilder init4_gsonBuilder() {
		return super.init4_gsonBuilder()
		// Slice as String (loses column position info, oh well)
		.registerTypeAdapter(Slice.class, new StandardAdapters.CharSequenceTypeAdapter(Slice.class));
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
		AppUtils.initESMappings(KStatus.main(), dbclasses, mappingFromClass);		
		// gsheet
		Dep.set(GSheetsClient.class, new GSheetsClient());
	}
}

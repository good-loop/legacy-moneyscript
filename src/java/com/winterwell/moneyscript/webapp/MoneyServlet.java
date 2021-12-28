package com.winterwell.moneyscript.webapp;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.winterwell.gson.Gson;
import com.winterwell.moneyscript.data.PlanDoc;
import com.winterwell.moneyscript.data.PlanSheet;
import com.winterwell.moneyscript.lang.Lang;
import com.winterwell.moneyscript.lang.cells.Scenario;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.nlp.simpleparser.ParseExceptions;
import com.winterwell.nlp.simpleparser.ParseFail;
import com.winterwell.utils.Dep;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.ajax.JSend;
import com.winterwell.web.ajax.JThing;
import com.winterwell.web.ajax.KAjaxStatus;
import com.winterwell.web.app.AppUtils;
import com.winterwell.web.app.IServlet;
import com.winterwell.web.app.WebRequest;
import com.winterwell.web.app.WebRequest.KResponseType;
import com.winterwell.web.fields.AField;
import com.winterwell.web.fields.JsonField;
import com.winterwell.web.fields.ListField;
import com.winterwell.web.fields.MissingFieldException;

public class MoneyServlet implements IServlet {

	private static final ListField<Scenario> SCENARIOS = new ListField<Scenario>(
			"scenarios", new AStringField("", Scenario.class))
			.setSplitPattern(",");
	static Lang lang = new Lang();
	
	@Override
	public void process(WebRequest state) throws Exception {
		List<PlanSheet> sheets = getSheets(state);
		try {			
			Business biz = lang.parse(sheets);
			
			// scenarios?
			List<Scenario> scs = state.get(SCENARIOS);
			if (scs!=null) {
				biz.setScenarios(scs);
			}
			
			// parse only?
			if (state.actionIs("parse")) {
				Map pi = biz.getParseInfoJson();
				JThing jt = new JThing().setJsonObject(pi);
				JSend jsend = new JSend(jt);
				jsend.send(state);				
			}
			
			// run!
			biz.run();
		
			// what format to return?
			if (state.getResponseType() == KResponseType.csv) {
				String csv = biz.toCSV();
				WebUtils2.send2(KResponseType.csv, state, csv);
				return;
			}
			ArrayMap json = biz.toJSON();
			JThing jt = new JThing().setJsonObject(json);
			JSend jsend = new JSend(jt);
			jsend.send(state);
		} catch(ParseFail pf) {
			processFail(Arrays.asList(pf), state);
		} catch(ParseExceptions pex) {
			processFail(pex.getErrors(), state);
		}
	}

	private List<PlanSheet> getSheets(WebRequest state) {
		PlanDoc _plandoc = new PlanDocServlet().getThing(state);
		if (_plandoc != null) {
			List<PlanSheet> sheets = _plandoc.getSheets();
			return sheets;
		}
		String text = state.get("text");
		if (text==null) {
			throw new MissingFieldException(AppUtils.ITEM);
		}
		PlanSheet sheet = new PlanSheet(text);
		return Arrays.asList(sheet);
	}

	private void processFail(List<ParseFail> pfs, WebRequest state) {
		JSend jsend = new JSend();
		jsend.setStatus(KAjaxStatus.fail);
		List data = Containers.apply(pfs, pf -> pf.toJson2());
		jsend.setData(new ArrayMap("errors", data));
		jsend.send(state);
	}

}

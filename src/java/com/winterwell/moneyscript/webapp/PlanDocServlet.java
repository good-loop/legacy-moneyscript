package com.winterwell.moneyscript.webapp;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.goodloop.gsheets.GSheetsClient;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.winterwell.data.KStatus;
import com.winterwell.es.ESPath;
import com.winterwell.es.client.KRefresh;
import com.winterwell.moneyscript.data.PlanDoc;
import com.winterwell.moneyscript.lang.GroupRule;
import com.winterwell.moneyscript.lang.Lang;
import com.winterwell.moneyscript.lang.Rule;
import com.winterwell.moneyscript.lang.UncertainNumerical;
import com.winterwell.moneyscript.lang.cells.CellSet;
import com.winterwell.moneyscript.lang.cells.CurrentRow;
import com.winterwell.moneyscript.lang.cells.RowName;
import com.winterwell.moneyscript.lang.num.Numerical;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.BusinessContext;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Col;
import com.winterwell.moneyscript.output.Row;
import com.winterwell.nlp.simpleparser.ParseExceptions;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.IHasJson;
import com.winterwell.web.WebEx.E403;
import com.winterwell.web.ajax.AjaxMsg;
import com.winterwell.web.ajax.JThing;
import com.winterwell.web.app.AppUtils;
import com.winterwell.web.app.CrudServlet;
import com.winterwell.web.app.WebRequest;

public class PlanDocServlet extends CrudServlet<PlanDoc> {

	public PlanDocServlet() {
		super(PlanDoc.class);
		augmentFlag = true;
	}

	@Override
	protected void doBeforeSaveOrPublish(JThing<PlanDoc> _jthing, WebRequest stateIgnored) {
		super.doBeforeSaveOrPublish(_jthing, stateIgnored);
		_jthing.java().errors = new ArrayList();
	}

	
	@Override
	protected JThing<PlanDoc> doPublish(WebRequest state, KRefresh forceRefresh, boolean deleteDraft) throws Exception {
		// normal
		JThing<PlanDoc> pubd = super.doPublish(state, forceRefresh, deleteDraft);
		// plus export to google ?? do this async?
		try {
			doExportToGoogle(pubd.java(), state, forceRefresh, deleteDraft);
		} catch(Throwable ex) {
			state.addMessage(AjaxMsg.error(ex));
		}
		return pubd;
	}
	
	private void doExportToGoogle(PlanDoc pd, WebRequest state, KRefresh forceRefresh, boolean deleteDraft) throws Exception {
		GSheetsClient sc = new GSheetsClient();
		// get/create
		if (pd.getGsheetId() == null) {
			Log.i("Make G-Sheet...");
			Spreadsheet s = sc.createSheet(pd.getName());
			pd.setGsheetId(s.getSpreadsheetId());
			// publish again
			doPublish(state, forceRefresh, deleteDraft);
		}
		
		GSheetFromMS ms2gs = new GSheetFromMS(sc);
		ms2gs.doExportToGoogle(pd);
	}

	@Override
	protected void augment(JThing<PlanDoc> jThing, WebRequest state) {
		// parse and add parse-info
		PlanDoc plandoc = jThing.java();
		try {
			Lang lang = MoneyServlet.lang;			
			String text = plandoc.getText();
			Business biz = lang.parse(text);			
			Map pi = biz.getParseInfoJson();
			if ( ! Utils.isEmpty(plandoc.errors)) {
				plandoc.errors = null;
				jThing.setJava(plandoc);
			}
		} catch(ParseExceptions exs) {
			plandoc.errors = Containers.apply(exs.getErrors(), IHasJson::toJson2);
			Log.d("parse.error", exs);
			jThing.setJava(plandoc);
		} catch(Throwable ex) {
			Log.e("parse", ex);
			// NB same json format as ParseFail
			plandoc.errors = Arrays.asList(new ArrayMap(
				"@type", ex.getClass().getSimpleName(),
				"message", com.winterwell.utils.Printer.toString(ex, true)
			));
			jThing.setJava(plandoc);
		}
	}
	
	
	@Override
	protected JThing<PlanDoc> getThingFromDB(WebRequest state) throws E403 {
		// HACK file
		String sbit1 = state.getSlugBits(1);
		if (sbit1!=null && sbit1.startsWith("file-")) {
			String sf = FileUtils.safeFilename(sbit1.substring(5), true);
			File f = new File("plans", sf);
			String s = FileUtils.read(f);
			PlanDoc pd = new PlanDoc();
			pd.id = state.getSlug();
			pd.setText(s);
			return new JThing().setType(PlanDoc.class).setJava(pd);
		}
		// normal - db
		return super.getThingFromDB(state);
	}
}

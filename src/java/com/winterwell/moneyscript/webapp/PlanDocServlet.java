package com.winterwell.moneyscript.webapp;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.goodloop.gsheets.GSheetsClient;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.winterwell.data.KStatus;
import com.winterwell.es.ESPath;
import com.winterwell.moneyscript.data.PlanDoc;
import com.winterwell.moneyscript.lang.Lang;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Row;
import com.winterwell.nlp.simpleparser.ParseExceptions;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.IHasJson;
import com.winterwell.web.WebEx.E403;
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
	public void process(WebRequest state) throws Exception {
		// TODO a button on the client
		if (state.actionIs("export-to-google")) {
			doExportToGoogle(state);
			return;
		}
		super.process(state);
	}
	
	private void doExportToGoogle(WebRequest state) throws Exception {
		PlanDoc pd = getThingStateOrDB(state).java();		
		GSheetsClient sc = new GSheetsClient();
		// get/create
		if (pd.getGsheetId() == null) {
			Spreadsheet s = sc.createSheet();
			pd.setGsheetId(s.getSpreadsheetId());
			ESPath path = esRouter.getPath(dataspace,type, pd.getId(), KStatus.DRAFT);
			AppUtils.doSaveEdit(path, jthing, state);
		}
		
		Business biz = MoneyServlet.lang.parse(pd.getText());		
		List<List<Object>> values = new ArrayList();
		List<Object> headers = Arrays.asList("Row");
		values.add(headers);
		List<Row> rows = biz.getRows();
		for (Row row : rows) {
			List<Object> rowvs = new ArrayList();
			rowvs.add(row.getName());
			values.add(rowvs);
		}
		// update with data		
		sc.updateValues(pd.getGsheetId(), values);
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

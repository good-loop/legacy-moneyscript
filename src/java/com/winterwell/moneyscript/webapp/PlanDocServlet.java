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
import com.winterwell.moneyscript.lang.ExportCommand;
import com.winterwell.moneyscript.lang.GroupRule;
import com.winterwell.moneyscript.lang.Lang;
import com.winterwell.moneyscript.lang.LangMisc;
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
import com.winterwell.nlp.simpleparser.ParseResult;
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
import com.winterwell.web.app.CommonFields;
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
	protected void doSave(WebRequest state) {
		// HACK Is it a file??
		File f = getPlanFile(state);
		if (f==null) {
			super.doSave(state);
			return;
		} else {
			File fd = new File(f.getParentFile(), "~"+f.getName());
			PlanDoc thing = getThing(state);
			String text = thing.getText();
			FileUtils.write(fd, text);
		}
	}	
	
	@Override
	protected JThing<PlanDoc> doPublish(WebRequest state, KRefresh forceRefresh, boolean deleteDraft) throws Exception {
		// HACK Is it a file??
		File f = getPlanFile(state);		
		// normal
		JThing<PlanDoc> pubd;
		if (f==null) {
			pubd = super.doPublish(state, forceRefresh, deleteDraft);
		} else {
			pubd = getThingStateOrDB(state);
			// Save to file
			String text = pubd.java().getText();
			FileUtils.write(f, text);
		}
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
		// HACK gsheet export id?
		if (pd.getText().contains("export:")) {
			String[] lines = StrUtils.splitLines(pd.getText());
			for (String line : lines) {
				if ( ! line.startsWith("export")) continue;
				try {
					ParseResult<ExportCommand> pr = LangMisc.exportRow.parse(line);
					ExportCommand ec = pr.getX();
					pd.setGsheetId(ec.spreadsheetId);					
				} catch (Exception ex) {
					// whatevs
				}
			}						
		}
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
			// HACK gsheet export id
			if (biz.export != null) {
				plandoc.setGsheetId(biz.export.spreadsheetId);
			}
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
		File f = getPlanFile(state);
		if (f != null) {
			KStatus status = state.get(CommonFields.STATUS);
			File fd = new File(f.getParentFile(), "~"+f.getName());
			if (status==KStatus.DRAFT 
					&& fd.isFile() 
					&& fd.lastModified() > f.lastModified()) 
			{
				f = fd; // load from draft
			}
			String s = FileUtils.read(f);
			PlanDoc pd = new PlanDoc();
			String slug = state.getSlug();
			String id = state.getSlugBits(1);
			pd.id = id;
			pd.setText(s);
			return new JThing().setType(PlanDoc.class).setJava(pd);
		}
		// normal - db
		return super.getThingFromDB(state);
	}

	/**
	 * 
	 * @param state
	 * @return file or null
	 */
	private File getPlanFile(WebRequest state) {
		String sbit1 = state.getSlugBits(1);
		if (sbit1!=null && sbit1.startsWith("file-")) {
			String sf = FileUtils.safeFilename(sbit1.substring(5), true);
			File f = new File("plans", sf);
			return f;
		}
		return null;
	}
}

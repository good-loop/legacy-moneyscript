package com.winterwell.moneyscript.webapp;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.goodloop.gsheets.GSheetsClient;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.winterwell.bob.wwjobs.BuildHacks;
import com.winterwell.data.KStatus;
import com.winterwell.es.ESPath;
import com.winterwell.es.client.ESHit;
import com.winterwell.es.client.KRefresh;
import com.winterwell.moneyscript.data.PlanDoc;
import com.winterwell.moneyscript.lang.CompareCommand;
import com.winterwell.moneyscript.lang.ExportCommand;
import com.winterwell.moneyscript.lang.ImportCommand;
import com.winterwell.moneyscript.lang.ImportRowCommand;
import com.winterwell.moneyscript.lang.Lang;
import com.winterwell.moneyscript.lang.LangMisc;
import com.winterwell.moneyscript.output.Business;
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
import com.winterwell.youagain.client.AuthToken;
import com.winterwell.youagain.client.YouAgainClient;

public class PlanDocServlet extends CrudServlet<PlanDoc> {

	static File plansDir = new File(FileUtils.getWinterwellDir(), "moneyscript-plans");

	public PlanDocServlet() {
		super(PlanDoc.class);
		augmentFlag = true;
	}

	@Override
	protected void doBeforeSaveOrPublish(JThing<PlanDoc> _jthing, WebRequest stateIgnored) {		
		super.doBeforeSaveOrPublish(_jthing, stateIgnored);
		// clear away parse errors - they are freshly set each time
		_jthing.java().errors = new ArrayList();
	}

	@Override
	protected void doSave(WebRequest state) {
		super.doSave(state); // db
		// HACK Is it a file??
		File f = getPlanFile(state);
		if (f!=null) {
			File fd = new File(f.getParentFile(), "~"+f.getName());
			PlanDoc thing = getThing(state);
			String text = thing.getText();
			doSave2_file_and_git(state, text, fd);
		}
	}	
	
	@Override
	protected List<ESHit<PlanDoc>> doList2_securityFilter(List<ESHit<PlanDoc>> hits2, WebRequest state,
			List<AuthToken> tokens, YouAgainClient yac) 
	{
		return super.doList2_securityFilter2_teamGoodLoop(hits2, state, tokens, yac);
	}

	@Override
	protected JThing<PlanDoc> doPublish(WebRequest state, KRefresh forceRefresh, boolean deleteDraft) throws Exception {
		// HACK Is it a file??
		File f = getPlanFile(state);		
		// normal
		JThing<PlanDoc> pubd = super.doPublish(state, forceRefresh, deleteDraft);
		if (f != null) {
			// Save to file also
			String text = pubd.java().getText();
			doSave2_file_and_git(state, text, f);
		}
		// plus export to google ?? do this async?
		PlanDoc pd = pubd.java();
		doExportToGoogle(pd, state, forceRefresh, deleteDraft);
		// re-save the draft with export status (for display in the editor)
		pubd.setJava(pd);
		ESPath path = esRouter.getPath(dataspace, type, pubd.java().getId(), KStatus.DRAFT);		
		AppUtils.doSaveEdit(path, pubd, null, state);		
		return pubd;
	}
	
	private void doExportToGoogle(PlanDoc pd, WebRequest state, KRefresh forceRefresh, boolean deleteDraft) throws Exception {
		Business biz = pd.getBusiness();
		List<ExportCommand> exports = pd.getExportCommands(); //biz.filterByClass(biz.getAllRules(), ExportCommand.class);
		for (ExportCommand exportCommand : exports) {
			if ( ! exportCommand.isActive()) continue;
			try {				
				exportCommand.runExport(pd, biz);
			} catch(Throwable ex) {
				state.addMessage(AjaxMsg.error(ex));				
			}
		}
		// update PlanDoc with the export status
		// NB: export is only done on publish -- so allow old status (esp errors) to stick around
//		pd.setExportCommands(biz.getExportCommands());
	}

	@Override
	protected JThing<PlanDoc> augment(JThing<PlanDoc> jThing, WebRequest state) {
		// parse and add parse-info
		PlanDoc plandoc = jThing.java();
		try {			
			Business biz = plandoc.getBusiness();
			Map pi = biz.getParseInfoJson();
			if ( ! Utils.isEmpty(plandoc.errors)) {
				plandoc.errors = null;				
			}
			// test out the import commands
			if (biz.getImportCommands() != null) {
				for(ImportCommand ic : biz.getImportCommands()) {					
					try {
						if (ic instanceof ImportRowCommand || ic instanceof CompareCommand) {
							ic.fetch();
						} else {
							ic.run2_importRows(biz);
						}
					} catch(Exception ex) {
						ic.setError(ex); // NB: this set is probably not needed, as ImportCommand will already have done it for most cases. But its safe.
						Log.d(ex);
						// oh well -- error should get passed on below
					}
				}
			}
			plandoc.setImportCommands(biz.getImportCommands());
//			if (state.actionIs(ACTION_PUBLISH)) {
//				// NB: export is only done on publish -- so allow old status (esp errors) to stick around
//				plandoc.setExportCommands(biz.getExportCommands());
//			}
			// export -- trigger id from url
			List<ExportCommand> exports = plandoc.getExportCommands();
			if (exports != null) {
				for (ExportCommand export : exports) {
					export.getSpreadsheetId();					
				}
			}
			// make sure the json gets updated
			jThing.setJava(plandoc);			
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
		return jThing;
	}
	
	
	@Override
	protected JThing<PlanDoc> getThingFromDB(WebRequest state) throws E403 {
		// normal - db
		JThing<PlanDoc> jpd = super.getThingFromDB(state);
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
			if (f.isFile()) {
				String s = FileUtils.read(f);
				if (jpd==null) {
					// e.g. using local to look at a remote file, pulled in via github
					PlanDoc pd = new PlanDoc();
					String id = getId(state);
					pd.setId(id);
					Log.w(LOGTAG(),"Creating local PlanDoc for "+id);
					jpd = new JThing(pd);
				}
				PlanDoc pd = jpd.java();
				pd.setText(s);
				jpd.setJava(pd);
			}
		}
		return jpd;
	}

	
	@Override
	protected PlanDoc getThing(WebRequest state) {
		if (jthing!=null) {
			return jthing.java();
		}
		PlanDoc pd = super.getThing(state);
		if (pd != null) {
			pd.business = null; // fresh from json, force a re-parse
		}
		return pd;
	}
	
	/**
	 * 
	 * @param state
	 * @return file or null
	 */
	private File getPlanFile(WebRequest state) {
		String sbit1 = state.getSlugBits(1);
		if (Utils.isBlank(sbit1)) {
			return null;
		}
		String sf = FileUtils.safeFilename(sbit1, true);
		File f = new File(plansDir, sf);
		return f;
	}
}

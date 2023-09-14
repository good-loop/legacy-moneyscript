package com.winterwell.moneyscript.webapp;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.winterwell.bob.tasks.WinterwellProjectFinder;
import com.winterwell.data.AThing;
import com.winterwell.data.KStatus;
import com.winterwell.es.ESPath;
import com.winterwell.es.client.ESHit;
import com.winterwell.es.client.KRefresh;
import com.winterwell.moneyscript.data.PlanDoc;
import com.winterwell.moneyscript.lang.CompareCommand;
import com.winterwell.moneyscript.lang.ExportCommand;
import com.winterwell.moneyscript.lang.ImportCommand;
import com.winterwell.moneyscript.lang.ImportRowCommand;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.nlp.dict.Dictionary;
import com.winterwell.nlp.simpleparser.ParseExceptions;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.IHasJson;
import com.winterwell.web.WebEx.E403;
import com.winterwell.web.ajax.AjaxMsg;
import com.winterwell.web.ajax.JSend;
import com.winterwell.web.ajax.JThing;
import com.winterwell.web.ajax.KAjaxStatus;
import com.winterwell.web.app.AppUtils;
import com.winterwell.web.app.CrudServlet;
import com.winterwell.web.app.WebRequest;
import com.winterwell.youagain.client.AuthToken;
import com.winterwell.youagain.client.YouAgainClient;

public class PlanDocServlet extends CrudServlet<PlanDoc> {

	static File plansDir = new File(FileUtils.getWinterwellDir(), "moneyscript-plans");

	private void doRefreshImports(WebRequest state, PlanDoc planDoc) {
		Business biz = planDoc.getBusiness();
		List<ImportCommand> ics = biz.getImportCommands();
		for (ImportCommand importCommand : ics) {
			importCommand.clearCache();
		}
		state.addMessage("Cleared cached imports for "+ics.size()+" "+ics);
	}
	
	static final String ACTION_CLEAR_IMPORTS = "clear-imports";
	@Override
	protected void doAction(WebRequest state) throws Exception {
		super.doAction(state);
		
		if (state.actionIs(ACTION_CLEAR_IMPORTS)) {
			JThing<PlanDoc> jplanDoc = getThingStateOrDB(state);
			doRefreshImports(state, jplanDoc.java());
			JSend jsend = new JSend().setStatus(KAjaxStatus.success);			
			jsend.send(state);
			return;
		}	
	}
	
	
	public PlanDocServlet() {
		super(PlanDoc.class);
		augmentFlag = true;
		gitAuditTrail = true;
	}

	@Override
	protected void doBeforeSaveOrPublish(JThing<PlanDoc> _jthing, WebRequest stateIgnored) {		
		super.doBeforeSaveOrPublish(_jthing, stateIgnored);
		// clear away parse errors - they are freshly set each time
		_jthing.java().errors = new ArrayList();
	}

	@Override
	protected File doBeforeSaveOrPublish2_git(WebRequest state, AThing ting) {
		File f = super.doBeforeSaveOrPublish2_git(state, ting);
		if (f==null) return null;		
		// text file too for useful human-readable diff
		File ftxt = FileUtils.changeType(f, "txt");
		String text = ((PlanDoc)ting).getText();
		doSave2_file_and_git(state, text, ftxt);		
		return f;
	}
	
	@Override
	protected void doSave(WebRequest state) {
		super.doSave(state); // db
	}	
	
	@Override
	protected List<ESHit<PlanDoc>> doList2_securityFilter(List<ESHit<PlanDoc>> hits2, WebRequest state,
			List<AuthToken> tokens, YouAgainClient yac) 
	{
		List<ESHit<PlanDoc>> safeHits = super.doList2_securityFilter2_filterByShares(hits2, state, tokens, yac);
//		super.doList2_securityFilter2_teamGoodLoop(hits2, state, tokens, yac);
		return safeHits;
	}

	protected boolean doList3_securityFilter3_filterByShares2_isGLSecurityHack(WebRequest state) {
		return isGLSecurityHack(state) && WinterwellProjectFinder.isDev(state);
	}
	
	@Override
	protected JThing<PlanDoc> doPublish(WebRequest state, KRefresh forceRefresh, boolean deleteDraft) throws Exception {
		// normal
		JThing<PlanDoc> pubd = super.doPublish(state, forceRefresh, deleteDraft);
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
		Business biz = null;
		try {			
			biz = plandoc.getBusiness();
			Map pi = biz.getParseInfoJson(); // can throw ParseException, caught below
			if ( ! Utils.isEmpty(plandoc.errors)) { 
				// no exception was thrown so remove any old errors
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
		// HACK: add rowNames to the json
		if (biz != null) {
			Dictionary rowNames = biz.getRowNames();
			if (rowNames.size() > 0) {
				ArrayMap map = new ArrayMap(jThing.map());
				map.put("rowNames", rowNames.values());
				jThing.setMap(map);
			}
		}
		// done
		return jThing;
	}
	
	
	@Override
	protected JThing<PlanDoc> getThingFromDB(WebRequest state) throws E403 {
		// normal - db
		JThing<PlanDoc> jpd = super.getThingFromDB(state);
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
	
}

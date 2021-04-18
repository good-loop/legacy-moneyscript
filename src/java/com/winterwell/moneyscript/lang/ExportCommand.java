package com.winterwell.moneyscript.lang;

import com.goodloop.gsheets.GSheetsClient;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.winterwell.moneyscript.data.PlanDoc;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.webapp.GSheetFromMS;
import com.winterwell.nlp.simpleparser.ParseResult;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.IHasJson;

/**
 * 
 * @author daniel
 *
 */
public class ExportCommand 
extends ImportCommand // Hack! but they are pretty similar 
{

	public String spreadsheetId;
	
	/**
	 * csv or google or ??
	 */
	protected String format;

	public ExportCommand(String gSheetUrlOrId) {
		super(gSheetUrlOrId);
		spreadsheetId = GSheetsClient.getSpreadsheetId(gSheetUrlOrId);
		if (spreadsheetId==null) spreadsheetId = gSheetUrlOrId;
	}

	@Override
	public void run(Business b) {		
		super.run(b);
		throw new TodoException(this);
	}
	
	@Override
	public void run2_importRows(Business b) {		
		super.run2_importRows(b);
		throw new TodoException(this);
	}

	public void runExport(PlanDoc pd) throws Exception {
		GSheetsClient sc = new GSheetsClient(); 	
//		// get/create
//		if (pd.getGsheetId() == null) {
//			Log.i("Make G-Sheet...");
//			Spreadsheet s = sc.createSheet(pd.getName());
//			pd.setGsheetId(s.getSpreadsheetId());
//			// publish again
//			doPublish(state, forceRefresh, deleteDraft);
//		}
//		pd.setGsheetId(spreadsheetId);					
		
		// Export all or some?
		
		GSheetFromMS ms2gs = new GSheetFromMS(sc);
		ms2gs.doExportToGoogle(this, pd.getBusiness());

	}
}

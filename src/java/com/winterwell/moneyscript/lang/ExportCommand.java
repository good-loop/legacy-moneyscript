package com.winterwell.moneyscript.lang;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.goodloop.gsheets.GSheetsClient;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.winterwell.maths.datastorage.DataTable;
import com.winterwell.moneyscript.data.PlanDoc;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.webapp.GSheetFromMS;
import com.winterwell.nlp.simpleparser.ParseResult;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.io.CSVReader;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.IHasJson;

/**
 * 
 * @author daniel
 * @testedby ExportCommandTest
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

	public void runExport(PlanDoc pd, Business biz) throws Exception {		 
		if (rows.contains("annual")) {
			// just export the annuals
			runExport2_annualOnly(pd, biz);
			return;
		}
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
		GSheetsClient sc = sc();
		GSheetFromMS ms2gs = new GSheetFromMS(sc);
		ms2gs.doExportToGoogle(this, biz);

	}

	private GSheetsClient sc() {
		return new GSheetsClient();
	}

	void runExport2_annualOnly(PlanDoc pd, Business biz) throws IOException, GeneralSecurityException {
		ArrayMap jobj = biz.toJSON();
		List<String> cols = (List<String>) jobj.get("columns");
		List<Map> brows = (List) jobj.get("rows");
		Map<String,List<Map>> dataForRow = (Map) jobj.get("dataForRow");
		
		List<List<Object>> annualValues = new ArrayList();		
		// headers
		ArrayList headers = new ArrayList();
		if (pd!=null) {
			headers.add("From "+pd.name+" ("+pd.id+") "+pd.getLastModified()); // squeeze in our source data! 
		} else {
			headers.add(" ");
		}
		for(String col : cols) {
			if (isAnnualCol(col)) {
				headers.add(col);
			}
		}
		annualValues.add(headers);
		// rows
		for(Map row : brows) {
			String rn = (String) row.get("name");
			assert rn != null;
			ArrayList rowvs = new ArrayList();
			rowvs.add(rn);
			List<Map> data = dataForRow.get(rn);
			assert data.size() == cols.size()-1 : data.size()+" vs "+cols.size()+" "+cols;
			for(int i=0; i<cols.size(); i++) {
				String coli = cols.get(i);
				if ( ! isAnnualCol(coli)) continue;
				Map vm = data.get(i);
				Object v = vm.get("v");
				rowvs.add(v);
			}
			annualValues.add(rowvs);	
		}
//		String bcsv = biz.toCSV(); // nope, this doesnt include the annuals!
		
		GSheetsClient sc = sc();		
		List<List<Object>> cleanVs = sc.replaceNulls(annualValues);
		sc.clearSpreadsheet(spreadsheetId);
		sc.updateValues(spreadsheetId, cleanVs);
	}

	private boolean isAnnualCol(String col) {
		col = col.toLowerCase().trim();
		// NB: exclude the final "Total" (of all time)
		return col.contains("total") && col.length() > 5;
	}
}

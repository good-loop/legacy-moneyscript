package com.winterwell.moneyscript.lang;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.goodloop.gsheets.GSheetsClient;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.winterwell.gson.FlexiGson;
import com.winterwell.gson.Gson;
import com.winterwell.maths.datastorage.DataTable;
import com.winterwell.moneyscript.data.PlanDoc;
import com.winterwell.moneyscript.data.PlanSheet;
import com.winterwell.moneyscript.lang.cells.Scenario;
import com.winterwell.moneyscript.lang.time.TimeDesc;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Row;
import com.winterwell.moneyscript.webapp.GSheetFromMS;
import com.winterwell.nlp.simpleparser.ParseResult;
import com.winterwell.utils.Dep;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.CSVReader;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.web.IHasJson;
import com.winterwell.utils.web.WebUtils;

/**
 * 
 * @author daniel
 * @testedby ExportCommandTest
 */
public class ExportCommand 
{
	
	// NB: some code will use "import" from the parent
	public static final String LOGTAG = "export";

	boolean active = true;	

	KColFreq colFreq = KColFreq.MONTHLY_AND_ANNUAL;
	
	protected Throwable error;
	
	/**
	 * csv or google or ??
	 */
	protected String format;

	public TimeDesc from;

	Time lastGoodRun;

	protected String name;

	boolean overlap;
	
	protected boolean overwrite;
	
	/**
	 * If true, try to create excel formulae in the export
	 */
	public boolean preferFormulae;

	List<String> scenarios;

	public String sheetName;
	
	/**
	 * gsheet can be a sheet ID or "by-order" or "skip" 
	 */
	public Map<String,String> gsheetForPlanSheetId;
	
	public String spreadsheetId;

	protected String url;

	public ExportCommand(String gSheetUrlOrId) {
		if (WebUtils.URL_REGEX.matcher(gSheetUrlOrId).matches()) {
			url = gSheetUrlOrId;
			spreadsheetId = GSheetsClient.getSpreadsheetId(url);			
		} else {
			spreadsheetId = gSheetUrlOrId;
			url = GSheetsClient.getUrl(spreadsheetId);
		}
	}
	public List<String> getScenarios() {
		return scenarios;
	}

	public String getSpreadsheetId() {
		if (spreadsheetId==null) {
			spreadsheetId = GSheetsClient.getSpreadsheetId(url);
		}
		return spreadsheetId;
	}

	public boolean isActive() {
		return active;
	}

	private boolean isAnnualCol(String col) {
		col = col.toLowerCase().trim();
		// NB: exclude the final "Total" (of all time)
		return col.contains("total") && col.length() > 5;
	}
	
	public boolean isOverlap() {
		return overlap;
	}

	public void runExport(PlanDoc pd, Business biz) throws Exception {
		try {
			if (getScenarios() != null) {
				biz.setScenarios(getScenarios());
			}
			
			Time time = new Time();
			if (colFreq==null) colFreq = KColFreq.MONTHLY_AND_ANNUAL; // default
			if (colFreq==KColFreq.ONLY_ANNUAL) {
				// just export the annuals
				runExport2_annualOnly(pd, biz);
				// success
				error = null;
				lastGoodRun = time;				
				return;
			}			
			// Export all or overlap
			GSheetsClient sc = sc();
			// NB: don't save temporary allocations in case the target sheets change
			Map<String, String> _gsheetForPlanSheetId = gsheetForPlanSheetId;
			if (_gsheetForPlanSheetId==null) {
				_gsheetForPlanSheetId = new ArrayMap();
				for(PlanSheet planSheet : pd.getSheets()) _gsheetForPlanSheetId.put(planSheet.getId(), "by-order");
			}
			// fill in sheet IDs
			List<SheetProperties> sheetProps = sc.getSheetProperties(getSpreadsheetId());
			for(int i=0; i<pd.getSheets().size(); i++) {
				PlanSheet ps = pd.getSheets().get(i);
				String gs = _gsheetForPlanSheetId.get(ps.getId());
				if ("by-order".equals(gs)) {
					if (sheetProps.size() > i) {
						_gsheetForPlanSheetId.put(ps.getId(), ""+sheetProps.get(i).getSheetId());
					} else {
						
					}
				}
			}
			for(PlanSheet planSheet : pd.getSheets()) {
				String shid = _gsheetForPlanSheetId.get(planSheet.getId());
				if ("skip".equals(shid)) {
					continue;
				}
				sc.setSheet(shid==null? null : Integer.parseInt(shid));
				GSheetFromMS ms2gs = new GSheetFromMS(sc, this, biz);
				ms2gs.setIncYearTotals(colFreq==KColFreq.MONTHLY_AND_ANNUAL);
				ms2gs.setPlanSheet(planSheet);
				ms2gs.doExportToGoogle();
			}
			// success
			error = null;
			lastGoodRun = time;
		} catch (Throwable ex) {
			error = ex;
			Log.e(LOGTAG, ex);
			throw Utils.runtime(ex);
		}
	}

	void runExport2_annualOnly(PlanDoc pd, Business biz) throws IOException, GeneralSecurityException {
		assert from==null : "TODO from";
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
			Row pojorow = biz.getRow(rn);
			if (pojorow.getParent() != null) {
				// skip anything but top level!?
				continue;
			}
			ArrayList rowvs = new ArrayList();
			rowvs.add(rn);
			List<Map> data = dataForRow.get(rn);
			assert data.size() == cols.size() : data.size()+" vs "+cols.size()+" "+cols;
			for(int i=0; i<cols.size(); i++) {
				String coli = cols.get(i);
				if ( ! isAnnualCol(coli)) continue;
				Map vm = data.get(i);
				Object v = vm.get("v");
				rowvs.add(v);
			}
			annualValues.add(rowvs);	
		}
		
		GSheetsClient sc = sc();		
//		if ( ! Utils.isBlank(sheetId)) { TODO
//			sc.setSheet(Integer.valueOf(sheetId));
//		}
		List<List<Object>> cleanVs = sc.replaceNulls(annualValues);
		sc.clearSpreadsheet(spreadsheetId);
		sc.updateValues(spreadsheetId, cleanVs);
	}

	private GSheetsClient sc() {
		return new GSheetsClient();
	}

	public void setFrom(TimeDesc from) {
		this.from = from;
	}
	public void setScenarios(List<String> scenarios) {
		this.scenarios = scenarios;
	}

}

package com.winterwell.moneyscript.lang;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.goodloop.gsheets.GSheetsClient;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.common.cache.CacheBuilder;
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
import com.winterwell.nlp.dict.Dictionary;
import com.winterwell.nlp.dict.NameMapper;
import com.winterwell.nlp.simpleparser.ParseResult;
import com.winterwell.utils.Dep;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.containers.ListMap;
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

	public static final String EXCEL_WITH_MS_CELL_REFS = "excel0";

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
	
	public transient Map<PlanSheet,GSheetFromMS> gsheetfromms4planSheet;
	
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
		Log.d(LOGTAG, "runExport "+this+"...");
		// HACK
		if (preferFormulae) {
			colFreq = KColFreq.ONLY_MONTHLY; // FIXME handle annual too
		}
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
			// fill in sheet IDs
			List<SheetProperties> sheetProps = sc.getSheetProperties(getSpreadsheetId());
			//match up
			// NB: don't save temporary allocations in case the target sheets change
			Map<String, String> _gsheetForPlanSheetId = runExport2_matchSheets(pd, sheetProps);
			List<PlanSheet> sheets = pd.getSheets();
			// init the gsheet exporters (so we can do cell-references)
			gsheetfromms4planSheet = new ArrayMap();
			for(PlanSheet planSheet : sheets) {
				String shid = _gsheetForPlanSheetId.get(planSheet.getId());
				if ("skip".equals(shid)) {
					continue;
				}
				if ("by-order".equals(shid)) {
					// skip -- not enough sheets in the target
					Log.d(LOGTAG, "Skip export of "+planSheet);
					continue;
				}
				GSheetFromMS ms2gs = new GSheetFromMS(sc, this, biz, planSheet);
				ms2gs.setIncYearTotals(colFreq==KColFreq.MONTHLY_AND_ANNUAL);
				ms2gs.setupRows();
				gsheetfromms4planSheet.put(planSheet, ms2gs);
			}
			
			boolean fail = false;
			for(PlanSheet planSheet : sheets) {
				try {
					String shid = _gsheetForPlanSheetId.get(planSheet.getId());
					if ("skip".equals(shid)) {
						continue;
					}
					if ("by-order".equals(shid)) {
						// skip -- not enough sheets in the target
						continue;
					}
					// Set the GSheet client to point to the right tab
					sc.setSheet(shid==null? null : Integer.parseInt(shid));
					GSheetFromMS ms2gs = gsheetfromms4planSheet.get(planSheet);
					// Export!
					ms2gs.doExportToGoogle();
//					System.out.println(planSheet.getTitle()+"\t"+ms2gs.nonce);
				} catch(Exception ex) {
					error = ex;
					Log.e(LOGTAG, ex);
					fail = true;
				}
			}			
			if (fail) {
				throw error;
			}
			// success			
			error = null;
			lastGoodRun = time;
		} catch (Throwable ex) {
			error = ex;
//			Log.e(LOGTAG, ("repeat log")ex);
			throw Utils.runtime(ex);
		}
	}
	
	
	/**
	 * Modifies
	 * 
	 * @param pd
	 * @param _gsheetForPlanSheetId
	 * @param sheetProps
	 * @return 
	 * 
	 */
	private Map<String, String> runExport2_matchSheets(PlanDoc pd, List<SheetProperties> sheetProps) 
	{
		// NB: don't save temporary allocations in case the target sheets change
		Map<String, String> _gsheetIdForPlanSheetId = gsheetForPlanSheetId;
		if (_gsheetIdForPlanSheetId==null) {
			_gsheetIdForPlanSheetId = new ArrayMap();
		}

		List<String> ourNames = Containers.apply(pd.getSheets(), PlanSheet::getTitle);
		Map<String,Integer> id4theirName = new ArrayMap();
		for (SheetProperties sp : sheetProps) {
			Integer old = id4theirName.put(sp.getTitle(), sp.getSheetId());
			if (old != null) {
				Log.w(LOGTAG, "ambiguous title for sheet "+sp.getTitle()+" "+url);
				// oh well, carry on ??can we log this somewhere for the user to know??
			}
		}
		NameMapper nm = new NameMapper(id4theirName.keySet());
		List<String> unmatchedTheirSheetTitle = nm.addTheirNames(ourNames);
		Map<String, String> gsheetName4ourname = nm.getOurNames4TheirNames();
		for(int i=0; i<pd.getSheets().size(); i++) {
			PlanSheet ps = pd.getSheets().get(i);
			String gs = _gsheetIdForPlanSheetId.get(ps.getId());
			if (gs==null) {
				String theirSheetName = gsheetName4ourname.get(ps.getTitle());
				if (theirSheetName!=null) {
					Integer sheetId = id4theirName.get(theirSheetName);
					assert sheetId != null : theirSheetName;
					_gsheetIdForPlanSheetId.put(ps.getId(), ""+sheetId);
					// stash in PlanSheet
					ps.setGSheetMatch(sheetId, theirSheetName);
					continue;
				}
			}
			if (gs==null || "by-order".equals(gs)) {
				if (sheetProps.size() > i) {
					SheetProperties sp = sheetProps.get(i);
					_gsheetIdForPlanSheetId.put(ps.getId(), ""+sp.getSheetId());
					// stash in PlanSheet
					ps.setGSheetMatch(sp.getSheetId(), sp.getTitle());
				} else {
					// TODO make a new sheet!
					Log.w(LOGTAG, "Make a new sheet please in "+url);
					Object addSheetReq;
//						sc.addSheetTab(getSpreadsheetId(), ps.getText());
				}
			}
		}
		return _gsheetIdForPlanSheetId;
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
	
	
	public GSheetFromMS getGSheetFromMSForRow(Row row) {
		Business biz = Business.get();
		Map<Row, GSheetFromMS> g4r = biz.gSheetFromMSForRow;
		if (g4r==null) {
			g4r = new HashMap();
			biz.gSheetFromMSForRow = g4r;
		}
		GSheetFromMS g = g4r.get(row);
		if (g==null) {
			// which plansheet is this row in?
			PlanSheet plansheet = biz.getPlanSheetForRow(row);
			assert plansheet != null : row;
			g = gsheetfromms4planSheet.get(plansheet);
			assert g != null : row+" "+plansheet;
			g4r.put(row, g);
		}
		return g;
	}
	
	
	ExportCommand setPreferFormulae(boolean b) {
		this.preferFormulae = b;
		return this;
	}

}

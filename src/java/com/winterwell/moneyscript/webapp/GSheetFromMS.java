package com.winterwell.moneyscript.webapp;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.goodloop.gsheets.GSheetsClient;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.GridData;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.RowData;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.winterwell.moneyscript.data.PlanDoc;
import com.winterwell.moneyscript.data.PlanSheet;
import com.winterwell.moneyscript.lang.ExportCommand;
import com.winterwell.moneyscript.lang.ImportCommand;
import com.winterwell.moneyscript.lang.Rule;
import com.winterwell.moneyscript.lang.UncertainNumerical;
import com.winterwell.moneyscript.lang.num.Numerical;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Col;
import com.winterwell.moneyscript.output.Row;
import com.winterwell.nlp.dict.Dictionary;
import com.winterwell.nlp.dict.NameMapper;
import com.winterwell.utils.Dep;
import com.winterwell.utils.Printer;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.containers.IntRange;
import com.winterwell.utils.log.Log;

/**
 * 
 * NB: Don't merge with ExportCommand 'cos it makes sense to keep google-drive specific code separate
 * 
 * @testedby GSheetFromMSTest
 * @author daniel
 *
 */
public class GSheetFromMS {

	public void setIncYearTotals(boolean incYearTotals) {
		this.incYearTotals = incYearTotals;
	}
	private static final String LOGTAG = "GSheetFromMS";
	private GSheetsClient sc;
	/**
	 * The rows in the GSheet
	 */
	private List<Row> spacedRows;
	private Business biz;
	private ExportCommand ec;
	private ArrayList unmatched;

	public GSheetFromMS(GSheetsClient sc, ExportCommand exportCommand, Business biz) {
		this.sc = sc;
		this.ec = exportCommand;
		assert exportCommand != null;
		this.biz = biz;
	}
	
	boolean incYearTotals;
	private PlanSheet planSheet;

	public void doExportToGoogle() throws Exception {
		assert ec.getSpreadsheetId() !=null && true : ec;

		setupRows();
		
		List<List<Object>> values = calcValues(biz);
		
		// NB: a specific tab in the spreadsheet? Done before this class

		// update with data
		if ( ! ec.isOverlap()) {
			values = sc.replaceNulls(values);
		}
				
		// Clear out the data in GoogleSheets before rewriting
		// to avoid old data leaking through in odd places
		if ( ! ec.isOverlap()) {
			sc.clearSpreadsheet(ec.spreadsheetId);
		}
		
		// update!
		sc.updateValues(ec.spreadsheetId, values);
		
		// actuals in blue
		ArrayList reqs = new ArrayList();
		Request reqblack = sc.setStyleRequest(new IntRange(1, Integer.MAX_VALUE), null, Color.black, null);
		reqs.add(reqblack);
		List<List<Map>> styles = calcStyles();
		for(int r=0; r<styles.size(); r++) {
			List<Map> row = styles.get(r);
			if (row==null) continue;
			for(int c=0; c<row.size(); c++) {
				Map cell = row.get(c);
				if (cell==null) continue;
				if (r>999 || c>33) {
					continue; // API limits?!
				}
				Request reqblu = sc.setStyleRequest(new IntRange(r,r), new IntRange(c,c), Color.blue, null);
				reqs.add(reqblu);
			}
		}
		if ( ! reqs.isEmpty()) {
//			Object sheets = sc.getSheets();
			sc.doBatchUpdate(ec.spreadsheetId, reqs);
		}
		Log.i(LOGTAG, "Exported to "+sc.getUrl(ec.spreadsheetId));
	}

	List<List<Object>> calcValues(Business biz) {
		biz.isExportToGoogle = true;
		Dep.set(GSheetFromMS.class, this); // for cell refs
		
		// run
		biz.run();		
		
		// json based approach - reuses the logic for the M$ front-end TODO switch all to this
		List<List<Object>> values = calcValues2_fromJson(biz);
		return values;
	}


	private List<List<Object>> calcValues2_fromJson(Business biz2) {
		ArrayMap json = biz.toJSON2(incYearTotals);
//		Printer.out(json.keySet());
//		Object rows = json.get("rows");
		List<String> columns = (List) json.get("columns");
		Map dataForRow = (Map) json.get("dataForRow");
		List<List<Object>> values = new ArrayList();

		// TODO filter the columns?
		IntRange incCols = new IntRange(0, Integer.MAX_VALUE); // no filter!
		if (ec.from!=null && false) { // Need to count annual columns
			Cell context = null;
			Col scol = ec.from.getCol(context);
			incCols = new IntRange(scol.index, Integer.MAX_VALUE); // probably correct: cols.size() - 1);
		}
		
		// add date row
		List<Object> headers = new ArrayList();
		headers.add(""); // the row-name column
		for(int c=0; c<columns.size(); c++) {
			String col = columns.get(c);
			if ( ! incCols.contains(c)) {
				continue;
			}
			headers.add(col);
		}
		values.add(headers);
				
		// make a blank row object
		final List<Object> blanks = new ArrayList();
		// ...overwrite or not depending on export=overlap
		for(int i=0; i<headers.size(); i++) blanks.add(ec.isOverlap()? null : "");
								
		// convert		
		for (Row row : spacedRows) {
			if (row==null) {
				values.add(blanks);
				continue;
			}
			List<Object> rowvs = new ArrayList();
			rowvs.add(row.getName());
			List<Map> cells = (List) dataForRow.get(row.getName());
			for(int c=0; c<cells.size(); c++) {
				Map cell = cells.get(c);
				if ( ! incCols.contains(c)) {
					continue;
				}
				Number v = (Number) cell.get("v");
				if (v ==null) {
					rowvs.add(""); 
					continue;
				}
				if (ec.preferFormulae && cell.get("excel") != null) {
					// Avoid self-reference which would upset GSheets
					// NB: "A12" contains "A1"
//					Pattern p = Pattern.compile("\\b"+cellRef(cell.row, cell.col)+"\\b");
//					if ( ! p.matcher(v.excel).find()) {
					Object excel = cell.get("excel");
					rowvs.add("="+excel); // a formula	
					continue;					
				}
				rowvs.add(v.doubleValue());							
			} // ./cell
			values.add(rowvs);
		}
		
		return values;
	}
	
	

	List<List<Map>> calcStyles() {
		// assume calcValues has run!		
		List<List<Map>> values = new ArrayList();
		// add date row
		List<Col> cols = biz.getColumns();	
		// filter the columns?
		IntRange incCols = new IntRange(0, Integer.MAX_VALUE); // no filter!
		if (ec.from!=null) {
			Cell context = null;
			Col scol = ec.from.getCol(context);
			incCols = new IntRange(scol.index, Integer.MAX_VALUE); // probably correct: cols.size() - 1);
		}
		
		List<Map> headers = new ArrayList();
		headers.add(null); 
		for (Col col : cols) {
			if ( ! incCols.contains(col.index)) {
				continue;
			}
			headers.add(null);
		}
		values.add(headers);
				
		// make a blank row object
		final List<Map> blanks = new ArrayList();
		// ...overwrite or not depending on export=overlap
		for(int i=0; i<headers.size(); i++) blanks.add(null);
								
		// convert		
		for (Row row : spacedRows) {
			if (row==null) {
				values.add(blanks);
				continue;
			}
			Rule r0 = row.getRules().get(0);
			List<Map> rowvs = new ArrayList();
			rowvs.add(null);
			List<Cell> cells = row.getCells(); // ??
			for (Cell cell : cells) {
				if ( ! incCols.contains(cell.col.index)) {
					continue;
				}
				// HACK for blue imports
				Numerical cv = biz.getCellValue(cell);
				if (cv!=null && ImportCommand.isImported(cv)) {
					rowvs.add(new ArrayMap(
						"color", "blue"
					));
					continue;
				}
				// TODO??
				String css = biz.getCSSForCell(cell);
//				if (Utils.isBlank(css)) {
//					rowvs.add(null); 
//					continue;
//				}
//				if (css.contains("blue")) {	// HACK
//					rowvs.add(new ArrayMap(
//							"color", "blue"
//							)); // toExportString());
//				} else {
				rowvs.add(null);
//				}
			} // ./cell
			values.add(rowvs);
		}		
		return values;
	}

	
	void setupRows() throws Exception {
		List<Row> rows = biz.getRows();
		// filter by sheet
		if (planSheet!=null) {
			List<String> prows = biz.getRows4plansheet().get(planSheet.getId());
			Collection<String> rowNames = prows==null? new HashSet() : new HashSet(prows);
			rows = Containers.filter(rows, row -> rowNames.contains(row.getName()));
		}
		if (ec.isOverlap()) {
			setupRows2_overlap(ec, biz, rows);
			return;
		}		
		// HACK - space with a blank row?
		spacedRows = new ArrayList();
		int prevLineNum = 0;
		for (Row row : rows) {
			// HACK - space with a blank row?
			Rule r0 = row.getRules().get(0);
			int lineNum = r0.getLineNum();
			if (lineNum > prevLineNum+1) {				
				spacedRows.add(null);
			}
			prevLineNum = lineNum;
			spacedRows.add(row);
		}
	}

	private void setupRows2_overlap(ExportCommand ec, Business biz, List<Row> rows) throws Exception {
		ArrayList<String> sheetRows = new ArrayList();
		// get the 1st column as rows
		List<List<Object>> sdata = sc.getData(ec.spreadsheetId, "A:A", ec.sheetName);
		for (List rd : sdata) {
			if (rd.isEmpty()) {
				sheetRows.add(null);
				continue;
			}
			Object v = rd.get(0);
			sheetRows.add(""+v);
		}
		// convert TODO refactor out the map rows-to-rows code for -reuse
		List<String> ourROwNames = Containers.apply(rows, Row::getName);
		spacedRows = new ArrayList();
		unmatched = new ArrayList();
		Dictionary rowNames = biz.getRowNames();
		NameMapper nameMapper = new NameMapper(rowNames);
		for(String sr : sheetRows) {
			if (Utils.isBlank(sr)) {
				spacedRows.add(null);
				continue;
			}
			String ourRowName = nameMapper.run2_ourRowName(sr);
			if (ourRowName==null) {
				unmatched.add(sr);
				spacedRows.add(null);
				continue;
			}
			int i = ourROwNames.indexOf(ourRowName);
			assert i != -1 : ourRowName+" vs "+ourROwNames;
			Row row = rows.get(i);
			spacedRows.add(row);			
		}
		// remove the 1st dates row, which is always reset
		spacedRows.remove(0);
		Log.i(LOGTAG, "Unmatched: "+unmatched);
		Log.d(LOGTAG, sheetRows+" --> "+spacedRows);
	}

	public String cellRef(Row row, Col col) {
		String msref = row.getName()+":"+col.index; // TODO use this and deref it at export
		int ki = spacedRows.indexOf(row)+2; // +1 for 0 index and +1 for the header row
		return GSheetsClient.getBase26(col.index)+ki;
	}

	public static String excel(Numerical x) {
		return x.excel==null? 
				Double.toString(x.doubleValue()) 
				: x.excel;
	}

	/**
	 * Wrap in ()s if needed
	 * @param x
	 * @return
	 */
	public static String excelb(Numerical x) {
		String s = excel(x);
		if (s.contains(" ")) s = "("+s+")";
		return s;
	}

	public void setPlanSheet(PlanSheet planSheet) {
		this.planSheet = planSheet;
	}

}

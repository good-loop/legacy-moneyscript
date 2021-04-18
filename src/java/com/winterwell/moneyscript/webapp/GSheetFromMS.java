package com.winterwell.moneyscript.webapp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import com.goodloop.gsheets.GSheetsClient;
import com.winterwell.moneyscript.data.PlanDoc;
import com.winterwell.moneyscript.lang.ExportCommand;
import com.winterwell.moneyscript.lang.Rule;
import com.winterwell.moneyscript.lang.UncertainNumerical;
import com.winterwell.moneyscript.lang.num.Numerical;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Col;
import com.winterwell.moneyscript.output.Row;
import com.winterwell.utils.Dep;
import com.winterwell.utils.Utils;
import com.winterwell.utils.log.Log;

/**
 * @testedby GSheetFromMSTest
 * @author daniel
 *
 */
public class GSheetFromMS {

	private static final String LOGTAG = "GSheetFromMS";
	private GSheetsClient sc;
	private List<Row> spacedRows;

	public GSheetFromMS(GSheetsClient sc) {
		this.sc = sc;
	}

	public void doExportToGoogle(ExportCommand ec, Business biz) throws Exception {
		assert ec.spreadsheetId !=null : ec;
		
		List<List<Object>> values = updateValues(biz);
		
		// update with data
		values = sc.replaceNulls(values);
				
		// Always clear out the data in GoogleSheets before rewriting
		// to avoid old data leaking through in odd places
		sc.clearSpreadsheet(ec.spreadsheetId);
		
		// update
		sc.updateValues(ec.spreadsheetId, values);

	}

	List<List<Object>> updateValues(Business biz) {
		biz.isExportToGoogle = true;
		Dep.set(GSheetFromMS.class, this); // for cell refs
		
		setupRows(biz);
		
		// run
		biz.run();		
		
		List<List<Object>> values = new ArrayList();
		
		List<Col> cols = biz.getColumns();		
		List<Object> headers = new ArrayList();
		headers.add("Row");
		for (Col col : cols) {
			headers.add(col.getTimeDesc());
		}
		values.add(headers);
				
		// a blank row
		final List<Object> blanks = new ArrayList();
		for(int i=0; i<headers.size(); i++) blanks.add("");
								
		// convert		
		for (Row row : spacedRows) {
			if (row==null) {
				values.add(blanks);
				continue;
			}
			Rule r0 = row.getRules().get(0);
//			if ("debug UK Staff".contains(row.getName())) {
//				System.out.println(r0); // TODO this has a few rules, e.g. 4% pay rise -- so it doesnt go to formula
//			}
			List<Object> rowvs = new ArrayList();
			rowvs.add(row.getName());
			Collection<Cell> cells = row.getCells();
			for (Cell cell : cells) {
				Numerical v = biz.getCellValue(cell);
				if (v ==null) {
					rowvs.add(""); 
					continue;
				}
				if ( ! Utils.isBlank(v.excel)) {
					// Avoid self-reference which would upset GSheets
					// NB: "A12" contains "A1"
					Pattern p = Pattern.compile("\\b"+cellRef(cell.row, cell.col)+"\\b");
					if ( ! p.matcher(v.excel).find()) {
						rowvs.add("="+v.excel); // a formula	
						continue;
					}
				}
				if (v instanceof UncertainNumerical) {
					rowvs.add(v.doubleValue());	
				} else {
					rowvs.add(v.doubleValue()); // toExportString());
				}				
			} // ./cell
			values.add(rowvs);
		}
		
		Dep.set(GSheetFromMS.class, null);
		
		return values;
	}

	private void setupRows(Business biz) {
		List<Row> rows = biz.getRows();

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
//		// HACK: put some blanks at the end (to handle a few rows being removed at a time)
//		for(int i=0; i<10; i++) {
//			spacedRows.add(null);		
//		}
	}

	public String cellRef(Row row, Col col) {
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

}

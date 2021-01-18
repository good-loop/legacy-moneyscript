package com.winterwell.moneyscript.webapp;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.goodloop.gsheets.GSheetsClient;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.winterwell.es.client.KRefresh;
import com.winterwell.moneyscript.data.PlanDoc;
import com.winterwell.moneyscript.lang.GroupRule;
import com.winterwell.moneyscript.lang.Rule;
import com.winterwell.moneyscript.lang.UncertainNumerical;
import com.winterwell.moneyscript.lang.cells.CellSet;
import com.winterwell.moneyscript.lang.cells.CurrentRow;
import com.winterwell.moneyscript.lang.cells.RowName;
import com.winterwell.moneyscript.lang.num.Numerical;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Col;
import com.winterwell.moneyscript.output.Row;
import com.winterwell.utils.Dep;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.log.Log;
import com.winterwell.web.app.WebRequest;

/**
 * @testedby {@link GSheetFromMSTest}
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

	public void doExportToGoogle(PlanDoc pd) throws Exception {
		assert pd.getGsheetId()!=null : pd;
		
		// parse
		Business biz = MoneyServlet.lang.parse(pd.getText());
		List<List<Object>> values = updateValues(biz);
		
		// update with data		
		sc.updateValues(pd.getGsheetId(), values);

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
				} else if ( ! Utils.isBlank(v.excel) 
						&& ! cellRef(cell.row, cell.col).equals(v.excel)) 
				{
					rowvs.add("="+v.excel); // a formula	
				} else if (v instanceof UncertainNumerical) {
					rowvs.add(v.doubleValue());	
				} else {
					rowvs.add(v.doubleValue()); // toExportString());
				}				
			} // ./cell
			values.add(rowvs);
		}
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
		return x.excel==null? Double.toString(x.doubleValue()) : x.excel;
	}

}

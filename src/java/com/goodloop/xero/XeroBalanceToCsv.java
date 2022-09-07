/**
 * 
 */
package com.goodloop.xero;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.goodloop.gsheets.GSheetsClient;
import com.winterwell.maths.datastorage.DataTable;
import com.winterwell.utils.io.CSVReader;
import com.winterwell.utils.io.CSVWriter;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;

/**
 *  Fetch Balcnce from Xero and save into a csv. Punt it up into a g-drive sheet
 * Assumes: a valid oauth token is stashed locally
 * @author daniel
 *
 */
public class XeroBalanceToCsv {

	private static final String LOGTAG = "XeroBalanceToCsv";


	public static void main(String[] args) throws GeneralSecurityException, IOException {
		JXero jxero = new JXero();
		jxero.init();		

		Time start = new Time(2019,1,1);
		Time end = TimeUtils.getStartOfMonth(new Time()).minus(TUnit.DAY);
		DataTable<String> dt = jxero.fetchBalanceSheet(start, end, TUnit.MONTH);
		
		// Make a big csv
//		DataTable<String> dt = new DataTable<>(data);
		// ...load the old (why??)
		File csv = new File("data/balance-sheet.csv");
		if (csv.isFile()) {
			DataTable<String> oldData = new DataTable<>(new CSVReader(csv));			
			dt = DataTable.merge(dt, oldData);
		}
		
		// ... save
		dt.save(new CSVWriter(csv));
		Log.i(LOGTAG, "Saved to: "+csv);
		
		// punt it up to a server
		if (true) { // NB: This sheet is used! So we have to be careful not to muck it up 
			GSheetsClient gsc = new GSheetsClient();
			List<List<Object>> jarr = dt.toJson2();
			Log.i(LOGTAG, "Make G-Sheet...");
			String sid = "1dPDjhUJyjDLIAy2n_dk9XLP4K6w0dIeQWotyJAJQMBk";
	//		Spreadsheet s = gsc.createSheet("Xero Profit and Loss");
	//		s.getSpreadsheetId();	
			
			// FIXME this update won't replace number with ""
			// Which means (since the rows can shift) that we can get bad data.
			// Hack fix: delete the data in G-sheets, then run this.
			
			Object ok = gsc.updateValues(sid, jarr);
			System.out.println(ok);
			System.out.println(gsc.getUrl(sid));
		}
	}

		
}

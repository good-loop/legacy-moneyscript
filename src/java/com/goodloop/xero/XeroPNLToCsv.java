/**
 * 
 */
package com.goodloop.xero;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import com.goodloop.gsheets.GSheetsClient;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.winterwell.maths.datastorage.DataTable;
import com.winterwell.utils.io.CSVWriter;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;
import com.winterwell.web.WebEx;

/**
 * Fetch P&L from Xero and save into a csv. Punt it up into a g-drive sheet
 * Assumes: a valid oauth token is stashed locally
 * @author daniel
 *
 */
public class XeroPNLToCsv {

	private static final String LOGTAG = "XeroPNLToCsv";


	public static void main(String[] args) throws GeneralSecurityException, IOException {
		JXero jxero = new JXero();
		jxero.init();
		
		// only need to change the end time each time we fetch the data
		Time start = new Time(2019,01,01);
		Time end = TimeUtils.getStartOfMonth(new Time()).minus(TUnit.DAY); 		
		DataTable<String> dt = jxero.fetchProfitAndLoss(start, end, TUnit.MONTH); 
		
		// ... save
		File csv = new File("data/pnl.csv");
		dt.save(new CSVWriter(csv));
		Log.i(LOGTAG, "Saved to: "+csv);
		
		// punt it up to a server
		if (true) { // NB: This sheet is used! So we have to be careful not to muck it up 
			GSheetsClient gsc = new GSheetsClient();
			List<List<Object>> jarr = dt.toJson2();
			
			// GoogleSheets doesn't handle null values well, change all null values to an empty space
			List<List<Object>> cleanedJarr = gsc.replaceNulls(jarr);
			
			Log.i(LOGTAG, "Make G-Sheet...");
			String sid = "1LmTaxqLu9fZFlz10G6XOgzHaGNXUu6xoD-fisrOxBRs";
	//		Spreadsheet s = gsc.createSheet("Xero Profit and Loss");
	//		s.getSpreadsheetId();	
			
			// Always clear out the data in GoogleSheets before rewriting
			try {
				gsc.clearSpreadsheet(sid);
			} catch (GoogleJsonResponseException ex) {
				if (ex.getStatusCode()==403) {
					throw new WebEx.E403("Check the spreadsheet share settings: "+ex.getMessage());
				}
				throw ex;
			}
			
			Object ok = gsc.updateValues(sid, cleanedJarr);
			System.out.println(ok);
			System.out.println(gsc.getUrl(sid));
		}
		System.exit(0);
	}
		
}

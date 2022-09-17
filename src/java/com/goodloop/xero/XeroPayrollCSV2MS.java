package com.goodloop.xero;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.goodloop.gsheets.GSheetsClient;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.winterwell.maths.datastorage.DataTable;
import com.winterwell.maths.timeseries.TimeSlicer;
import com.winterwell.utils.FailureException;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.io.CSVReader;
import com.winterwell.utils.io.CSVWriter;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;
import com.winterwell.web.WebEx;

public class XeroPayrollCSV2MS {

	public static void main(String[] args) throws IOException {
		CSVReader r = new CSVReader(new File("data/PayrollActivityDetails.csv"));
		int n = 0;
		List<Payslip> payslips = new ArrayList<>();
		Payslip payslip = null;		
		for (String[] row : r) {
			// detect start of payslip
			String[] bits = row[0].split(" - ");
			if (bits.length==2) {
				Time t = TimeUtils.parseExperimental(bits[1].trim());
				payslip = new Payslip();
				payslip.date = t;
				String name = bits[0].trim();
				String n2 = new ArrayMap<String,String>(
					"Ben Durkin", "Vera Durkin",
					"Ana Carolina Ratti Gomes", "Carol Ratti",
					"Marie Lola Conrich", "Lola Conrich"
				).get(name);
				if (n2 !=null) name = n2;
				payslip.name = name;
				payslips.add(payslip);
			}
			if (payslip==null) continue;
			payslip.rows.add(row);
		}
		Printer.out(payslips.get(0));
		Printer.out(payslips.get(1));
		
		// Build a sheet of person / time		
		Time start=TimeUtils.WELL_FUTURE, end=TimeUtils.WELL_OLD;
		for (Payslip payslip2 : payslips) {
			if (payslip2.date.isAfter(end)) end = payslip2.date;
			if (payslip2.date.isBefore(start)) start = payslip2.date;
		}
		Map<String,List> dt = new HashMap<>();
		int si = start.getYear()*12 + start.getMonth();
		int ei = end.getYear()*12 + end.getMonth();
		int numMonths = 1 + ei - si;
		Printer.out(start, end, si, ei, numMonths);
		for (Payslip payslip2 : payslips) {
			List row = dt.get(payslip2.name);
			if (row==null) {
				row = new ArrayList(numMonths);
				dt.put(payslip2.name, row);
			}
			int i = payslip2.date.getYear()*12 + payslip2.date.getMonth();
			i -= si;
			while(row.size() <= i) row.add(null);
			row.set(i, payslip2.getEarnings());
		}
		Printer.out(Printer.toString(dt.get("Aidan Thomson"), ", "));
		Printer.out(Printer.toString(dt.get("Daniel Winterstein"), ", "));
		CSVWriter w = new CSVWriter(new File("data/PayrollActivityDetails.processed.csv"));
		List<List<Object>> jarr = new ArrayList();
		ArrayList h = new ArrayList();
		h.add(null);
		Time t = start;
		for(int i=0; i<numMonths; i++) {
			h.add(t.toISOStringDateOnly());
			t = t.plus(TUnit.MONTH);
		}
		w.write(h);
		jarr.add(h);
		for(String k : dt.keySet()) {
			List row = dt.get(k);
			row.add(0, k);
			w.write(row);
			jarr.add(row);
		}
		w.close();
		
		// upload		
		// punt it up to a server
		if (true) { // NB: This sheet is used! So we have to be careful not to muck it up 
			GSheetsClient gsc = new GSheetsClient();			
			
			// GoogleSheets doesn't handle null values well, change all null values to an empty space
			List<List<Object>> cleanedJarr = gsc.replaceNulls(jarr);
			
			String sid = "1PDqsFJcsBXgrHnQhKkrUaTPjqruk9U5NNsjE9OYMAhM";
			
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
	}
}

class Payslip {
	public String name;
	Time date;
	List<String[]> rows = new ArrayList();
	@Override
	public String toString() {
		return "Payslip[name="+name+", date=" + date +", earnings="+getEarnings()+", rows=" + Printer.toString(rows) + "]";
	}
	double getEarnings() {
		for (String[] row : rows) {
			// 5 blanks and a number
			boolean nope = false;
			for(int i=0; i<5; i++) {
				if ( ! Utils.isBlank(row[i])) {
					nope = true;
					break;
				}				
			}
			if (nope) continue;
			if (MathUtils.getNumber(row[5]) != 0) {
				double earnings = MathUtils.getNumber(row[5]);
				return earnings;							
			}
			throw new FailureException("Missed? "+Printer.toString(rows));
		}
		throw new FailureException(Printer.toString(rows));
	}
}


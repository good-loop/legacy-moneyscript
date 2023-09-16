package com.winterwell.moneyscript.lang;

import java.io.File;

import org.junit.Ignore;
import org.junit.Test;

import com.winterwell.moneyscript.lang.num.Numerical;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Col;
import com.winterwell.moneyscript.output.Row;
import com.winterwell.nlp.dict.Dictionary;
import com.winterwell.utils.Printer;
import com.winterwell.utils.time.Time;

public class ImportCommandTest {

	@Test
	public void testRowMatchingBug_Payroll() {
		String ms = "start: Jan 2021\n"
				+"import: https://docs.google.com/spreadsheets/d/1PDqsFJcsBXgrHnQhKkrUaTPjqruk9U5NNsjE9OYMAhM {rows:all}\n"
				;
		Lang lang = new Lang();
		Business b = lang.parse(ms);
		
//		ImportCommand ic = new ImportCommand("https://docs.google.com/spreadsheets/d/1PDqsFJcsBXgrHnQhKkrUaTPjqruk9U5NNsjE9OYMAhM");
//		ic.setRows(Arrays.asList(ic.ALL_ROWS));
//		ic.run(b);
		
		b.run();
		System.out.println(b.toCSV());
	}

	@Test
	public void testRowMatchingBug_Advertising() {

//		Mostly works!
//		But with a row matching error
//		Advertising Inventory << which is in our plan
//		picks up the values for
//		Advertising & Marketing - excluding AdOps << which is not in our plan!!
//		How can we make the first-word match just a little more careful?
//		Why isn't the matches.size() == 1 anti-ambiguity safety check working?
//		
		String ms = "start: Jan 2019\n"
				+"import: https://docs.google.com/spreadsheets/d/1LmTaxqLu9fZFlz10G6XOgzHaGNXUu6xoD-fisrOxBRs\n"
				+"Advertising Inventory: £100 per month\n";
		Lang lang = new Lang();
		Business b = lang.parse(ms);
		b.run();
		Dictionary rows = b.getRowNames();		
		assert rows.contains("Advertising Inventory");	
		Row ai = b.getRow("Advertising Inventory");
		double[] vs = ai.getValues();
		assert vs[0] == 29527.07; // NB might be vs[1]
	}
	
	
//	@Test old url
	public void testImportGL() {
		String ms = "import: https://docs.google.com/spreadsheets/d/e/2PACX-1vRvLd73E4kTwaoV3PRzQeDnJT7A1VZGzj6DjQty4sPckoikUEdqsuR0lkRCjVLFSWReywOfX5vtgif5/pub?output=csv {url: https://docs.google.com/spreadsheets/d/1qDa7ZuGr3g7OvVycUaE2WiwL8diZ0YRBPYrNi8TYrIU, name:\"actuals taken from NEW Reasonable Estimate PLUS SE Funding_INC Revised Spending_15.09.20\", rows:\"overlap\"}\n"
					+"Balance: 100 per month\n"
					+"Amy: £100 per month\n";
		Lang lang = new Lang();
		Business b = lang.parse(ms);
		b.run();
		Dictionary rows = b.getRowNames();
		assert rows.contains("Balance");
		assert rows.contains("Amy");
		assert rows.size() == 2 : rows;
	}			


	
	@Test
	public void testImportBadUrl() {
		String ms = "import: https://bbc.co.uk/foobar.nope\n"
					+"Balance: 100 per month\n"
					+"Amy: £100 per month\n";
		Lang lang = new Lang();
		Business b = lang.parse(ms);
		try {
			b.run();
			assert false;
		} catch (Exception ex) {
			assert ex != null; // an E404
		}				
		ImportCommand ic = b.getImportCommands().get(0);
		assert ic.getError() != null;
	}			
	
	@Test @Ignore // the date columns were 2 cells which is hard for a computer. solution - nicer inputs
	public void testImportTooComplex() {
		String ms = "start: Jan 2021\n"
					+"import: https://docs.google.com/spreadsheets/d/1Z1iDN77Zugmn9P_nOgarjPeMyuHdG0iX08MfuIqTS5M/edit#gid=1505120226 {rows:all}\n"
					+"Balance: 100 per month\n"
					+"Amy: £100 per month\n";
		Lang lang = new Lang();
		Business b = lang.parse(ms);
		b.run();
		Dictionary rows = b.getRowNames();
		assert rows.contains("Balance");
		assert rows.contains("Amy");
		Printer.out(rows);
		assert rows.size() > 2 : rows;
		
		Row gmr = b.getRow("Group M");
		double[] vs = gmr.getValues();
		Printer.out(vs);
		Col col = new Col(7);
		Cell cell = new Cell(gmr, col);
		Numerical cv = b.getCellValue(cell);
		assert cv.doubleValue() == 62500 : cv;
	}			
	
	@Test
	public void testBusinessRun() {
		ImportCommand ic = new ImportCommand(new File("test/test-input.csv").toURI().toString());
		ic.overwrite = true;
		ic.setRows("all");
		Business bs = new Business();
		bs.addRow(new Row("Dummy"), null);
		bs.getSettings().setStart(new Time(new Time().getYear(), 1, 1));
		bs.getSettings().setEnd(new Time(new Time().getYear(), 12, 31));
		bs.addImportCommand(ic);
		bs.run();
		System.out.println(bs.toCSV());
		System.out.println(bs.toJSON());
	}


}

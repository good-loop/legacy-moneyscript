package com.winterwell.moneyscript.lang;

import java.io.File;

import org.junit.Test;

import com.winterwell.moneyscript.output.Business;
import com.winterwell.nlp.dict.Dictionary;
import com.winterwell.utils.time.Time;

public class ImportCommandTest {

	@Test
	public void testImportGL() {
		String ms = "import: https://docs.google.com/spreadsheets/d/e/2PACX-1vRvLd73E4kTwaoV3PRzQeDnJT7A1VZGzj6DjQty4sPckoikUEdqsuR0lkRCjVLFSWReywOfX5vtgif5/pub?output=csv {url: https://docs.google.com/spreadsheets/d/1qDa7ZuGr3g7OvVycUaE2WiwL8diZ0YRBPYrNi8TYrIU, name:\"actuals taken from NEW Reasonable Estimate PLUS SE Funding_INC Revised Spending_15.09.20\", rows:\"overlap\"}\n"
					+"Balance: 100 per month\n";
		Lang lang = new Lang();
		Business b = lang.parse(ms);
		b.run();
		Dictionary rows = b.getRowNames();
		assert rows.contains("Balance");
		assert rows.size() == 1 : rows;
	}			

	@Test
	public void testBusinessRun() {
		ImportCommand ic = new ImportCommand(new File("test/test-input.csv").toURI().toString());
		ic.overwrite = true;
		Business bs = new Business();
		bs.getSettings()._start = new Time(new Time().getYear(), 1, 1);
		bs.getSettings()._end = new Time(new Time().getYear(), 12, 31);
		bs.addImportCommand(ic);
		bs.run();
		System.out.println(bs.toCSV());
		System.out.println(bs.toJSON());
	}

}

package com.winterwell.moneyscript.lang;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.winterwell.moneyscript.lang.num.Formula;
import com.winterwell.moneyscript.lang.num.LangNum;
import com.winterwell.moneyscript.lang.num.Numerical;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Row;
import com.winterwell.nlp.dict.Dictionary;
import com.winterwell.nlp.simpleparser.ParseResult;
import com.winterwell.utils.Printer;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.time.Time;

public class ImportCommandTest {


	
	@Test
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
	public void testBusinessRun() {
		ImportCommand ic = new ImportCommand(new File("test/test-input.csv").toURI().toString());
		ic.overwrite = true;
		ic.setRows("all");
		Business bs = new Business();
		bs.addRow(new Row("Dummy"));
		bs.getSettings().setStart(new Time(new Time().getYear(), 1, 1));
		bs.getSettings().setEnd(new Time(new Time().getYear(), 12, 31));
		bs.addImportCommand(ic);
		bs.run();
		System.out.println(bs.toCSV());
		System.out.println(bs.toJSON());
	}


}

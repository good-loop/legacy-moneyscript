package com.winterwell.moneyscript.lang;

import java.io.File;

import org.junit.Test;

import com.winterwell.moneyscript.lang.num.Formula;
import com.winterwell.moneyscript.lang.num.LangNum;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Row;
import com.winterwell.nlp.dict.Dictionary;
import com.winterwell.nlp.simpleparser.ParseResult;
import com.winterwell.utils.Printer;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.time.Time;

public class ImportRowCommandTest {


	@Test
	public void testImportRowFromSF_lowLevel() {
		// sum(Net Amount) using {Start Date: month} 
		ImportRowCommand ic = new ImportRowCommand("file:///home/daniel/winterwell/moneyscript/data/SF-won-report.csv");
		
		Lang lang = new Lang();
		ParseResult<Formula> pf = LangNum.num.parseOut("sum(Net Amount)");
		ic.formula = pf.getX();
		ic.setMapping(new ArrayMap("Start Date", "month"));
		
		String ms = "Alice: 1";
		
		Business b = lang.parse(ms);
		b.getSettings().setStart(new Time(2020,1,1));
		b.getSettings().setEnd(new Time(2020,6,30));
		ic.run(b);
		String vs = ic.values.toString();
		Printer.out(vs);
		assert vs.equals("[null, 268k, 69.9k, 28.1k, 16.7k, 59.6k]");
	}
	
	
	@Test
	public void testImportRowFromSF() {
		String ms = "SF Net Amount: import sum(Net Amount) using Start Date from file:///home/daniel/winterwell/moneyscript/data/SF-won-report.csv"
				+" {name:\"SF exported csv\"}\n";
		Lang lang = new Lang();
		Business b = lang.parse(ms);
		b.run();
		Row sfna = b.getRow("SF Net Amount");
		System.out.println(b.toCSV());
	}			

	
}

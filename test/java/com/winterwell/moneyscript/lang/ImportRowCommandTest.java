package com.winterwell.moneyscript.lang;

import org.junit.Test;

import com.winterwell.moneyscript.lang.num.Formula;
import com.winterwell.moneyscript.lang.num.LangNum;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Row;
import com.winterwell.nlp.simpleparser.PP;
import com.winterwell.nlp.simpleparser.ParseResult;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.time.Time;

public class ImportRowCommandTest {

	
	@Test
	public void testImportRowFromSF_count() {
		{
			String ms = "NumCampaigns: import by month count(Amount) using {\"End Date\": month} from https://docs.google.com/spreadsheets/d/1FR8Y8LBgMF_aFxTXNPWl9Rc6WQZkdLBckwE61lMLJOw/gviz/tq?tqx=out:csv";
			Lang lang = new Lang();
			Business b = lang.parse(ms);
			b.getSettings().setStart(new Time(2020,1,1));
			b.getSettings().setEnd(new Time(2021,12,31));
			b.run();
			Row sfna = b.getRow("NumCampaigns");						
			double[] vs = sfna.getValues();
			System.out.println(b.toCSV());
			assert MathUtils.equalish(vs[9], 9) : vs[9]; // Oct
		}	
	}

	@Test
	public void testParse_ImportRowFromSF() {
		Lang lang = new Lang();
		ParseResult<Formula> f = LangNum.num.parseOut("TotalBank");
		System.out.println(f);
		ParseResult<ImportRowCommand> r = lang.langMisc.importRow.parseOut(
				"import by month count(Amount) using {\"End Date\": month} from https://docs.google.com/spreadsheets/d/1FR8Y8LBgMF_aFxTXNPWl9Rc6WQZkdLBckwE61lMLJOw/gviz/tq?tqx=out:csv");
	}

	@Test
	public void testParse_ImportRowBankBalance() {
		Lang lang = new Lang();	
		PP<ImportRowCommand> p = lang.langMisc._importRow2;
//		p.DEBUG = true;
		ParseResult<ImportRowCommand> r = p.parseOut(
				"import Total Bank from https://docs.google.com/spreadsheets/d/1dPDjhUJyjDLIAy2n_dk9XLP4K6w0dIeQWotyJAJQMBk");
	}

	@Test
	public void testImportRowBankBalance() {
		Lang lang = new Lang();
//		{
//			ParseResult<Formula> f = LangNum.num.parseOut("TotalBank");		
//			System.out.println(f);
//			ParseResult<ImportRowCommand> r = lang.langMisc.importRow.parseOut("import Total Bank from https://docs.google.com/spreadsheets/d/1dPDjhUJyjDLIAy2n_dk9XLP4K6w0dIeQWotyJAJQMBk");
//		}
		{
			String ms = "CashAtBank: import Total Bank from https://docs.google.com/spreadsheets/d/1dPDjhUJyjDLIAy2n_dk9XLP4K6w0dIeQWotyJAJQMBk";
			Business b = lang.parse(ms);
			// 2020
			b.getSettings().setStart(new Time(2020,1,1));
			b.getSettings().setEnd(new Time(2020,12,31));
			b.run();
			Row sfna = b.getRow("CashAtBank");						
			double[] vs = sfna.getValues();
			System.out.println(b.toCSV());
			assert MathUtils.equalish(vs[10], 829829.78) : vs[10]; // Nov 2020
		}	
	}
	
	@Test
	public void testImportRowTimeCap() {
		{	// time limit
			String ms = "Blah to Mar 2020: 1\nNumCampaigns to Mar 2020: import by month count(Amount) using {\"End Date\": month} from https://docs.google.com/spreadsheets/d/1FR8Y8LBgMF_aFxTXNPWl9Rc6WQZkdLBckwE61lMLJOw/gviz/tq?tqx=out:csv";
			Lang lang = new Lang();
			Business b = lang.parse(ms);
			b.getSettings().setStart(new Time(2020,1,1));
			b.getSettings().setEnd(new Time(2021,12,31));
			b.run();
			Row sfna = b.getRow("NumCampaigns");						
			double[] vs = sfna.getValues();
			System.out.println(b.toCSV());
			assert vs[9]==0 : vs[9]; // Oct
		}	
	}
	
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
		assert vs.equals("[null, 107k, 12.4k, 0, 81.6k, 156k, 83.3k]") : vs;
	}
	
	
	@Test
	public void testImportRowFromSF() {
		// This syntax is too fragile -- TODO allow reordering sentence chunks
		String ms = "SF Net Amount: import by month sum(Net Amount) using {\"End Date\":month, name:\"SF exported csv\"} from file:///home/daniel/winterwell/moneyscript/data/SF-won-report.csv";
		Lang lang = new Lang();
		PP<ImportRowCommand> p = lang.langMisc._importRow1;
//		Parser.DEBUG = true;
		Business b = lang.parse(ms);
		b.getSettings().setStart(new Time(2020,1,1));
		b.getSettings().setEnd(new Time(2020,12,31));
		b.run();
		Row sfna = b.getRow("SF Net Amount");
		System.out.println(b.toCSV());
		double[] vs = sfna.getValues();
		assert MathUtils.equalish(vs[9], 189443) : vs[9]; // Oct
	}			

	
	
	@Test 
	public void testParseImportFromSF_https() {
		{
			String ms = "import aggregate mean(Net Amount) from https://docs.google.com/spreadsheets/d/e/2PACX-1vRy.csv";
			Lang lang = new Lang();
			PP<ImportRowCommand> p = lang.langMisc._importRow1;
			p.parseOut(ms);
		}
		{
			String ms = "import aggregate mean(Net Amount) from https://docs.google.com/spreadsheets/d/e/2PACX-1vRyHr0yWj22C_2Q_DiS_eC3z0IRdRslHRXn3yy68cdIbW3If_DzwNnIyTWH-PQTrF4BDa1S_WsanH00/pub?output=csv";
			Lang lang = new Lang();
			PP<ImportRowCommand> p = lang.langMisc._importRow1;
			p.parseOut(ms);
		}
		{
			String ms = "Campaign_Size: import aggregate mean(Net Amount) from https://docs.google.com/spreadsheets/d/e/2PACX-1vRyHr0yWj22C_2Q_DiS_eC3z0IRdRslHRXn3yy68cdIbW3If_DzwNnIyTWH-PQTrF4BDa1S_WsanH00/pub?output=csv  // last I checked it was about N(25k, std-dev 25k). Note that from the data: existing clients don't have a different £ profile.";
			Lang lang = new Lang();
			Business b = lang.parse(ms);
		}
	}

	@Test
	public void testImportRowFromSF_fns() {
		{
			String ms = "SF Net Amount: import by month count(Amount) using {\"End Date\":month, name:\"SF exported csv\"}"
					+ " from file:///home/daniel/winterwell/moneyscript/data/SF-won-report.csv";
			Lang lang = new Lang();
			PP<ImportRowCommand> p = lang.langMisc._importRow1;
//			Parser.DEBUG = true;
			Business b = lang.parse(ms);
			b.getSettings().setStart(new Time(2020,1,1));
			b.getSettings().setEnd(new Time(2020,12,31));
			b.run();
			Row sfna = b.getRow("SF Net Amount");
			System.out.println(b.toCSV());
			double[] vs = sfna.getValues();
			assert MathUtils.equalish(vs[9], 9) : vs[9]; // Oct
		}
		{
			String ms = "SF Net Amount: import by month count(Opportunity Name) using {\"End Date\":month, name:\"SF exported csv\"}"
					+ " from file:///home/daniel/winterwell/moneyscript/data/SF-won-report.csv";
			Lang lang = new Lang();
			PP<ImportRowCommand> p = lang.langMisc._importRow1;
//			Parser.DEBUG = true;
			Business b = lang.parse(ms);
			b.getSettings().setStart(new Time(2020,1,1));
			b.getSettings().setEnd(new Time(2020,12,31));
			b.run();
			Row sfna = b.getRow("SF Net Amount");
			System.out.println(b.toCSV());
			double[] vs = sfna.getValues();
			assert MathUtils.equalish(vs[9], 9) : vs[9]; // Oct
		}
		{
			String ms = "AvgNetAmount: import aggregate mean(Net Amount) from file:///home/daniel/winterwell/moneyscript/data/SF-won-report.csv";
			Lang lang = new Lang();
	//		Parser.DEBUG = true;
			Business b = lang.parse(ms);
			b.getSettings().setStart(new Time(2020,1,1));
			b.getSettings().setEnd(new Time(2020,12,31));
			b.run();
			Row sfna = b.getRow("SF Net Amount");
			System.out.println(b.toCSV());
		}
	}			

	
}

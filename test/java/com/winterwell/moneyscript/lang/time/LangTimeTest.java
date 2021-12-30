package com.winterwell.moneyscript.lang.time;

import org.junit.Test;

import com.winterwell.moneyscript.lang.Lang;
import com.winterwell.moneyscript.lang.cells.LangCellSet;
import com.winterwell.moneyscript.lang.num.SimpleLangNum;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.BusinessContext;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Col;
import com.winterwell.nlp.simpleparser.GrammarPrinter;
import com.winterwell.nlp.simpleparser.ParseResult;
import com.winterwell.nlp.simpleparser.Parser;
import com.winterwell.utils.Printer;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;


public class LangTimeTest {

	@Test
	public void testAMonth() {
		Parser.clearGrammar();
		Lang lang = new Lang();
		LangTime lt = new LangTime();
//		lt.date.parseOut("June 2020");
		Parser.DEBUG = true;
		Business p = lang.parse("Stuff from June 2020: £1");
		System.out.println(p);
		lt.complexTime.parseOut("1 month from June 2020");
		
		// NB: this actually uses conditionalFilter
		lang.parse("White papers from June 2020 to June 2020: £3k");
		
		lang.parse("White papers at June 2020: £3k");
		
//		lang.parse("White papers: £3k at June 2020"); fails
	}			
	

	@Test
	public void testNotAMonth() {
		Parser.clearGrammar();
		Lang lang = new Lang();
		LangTime lt = new LangTime();
//		lt.date.parseOut("June 2020");r
//		Parser.DEBUG = true;
		ParseResult<TimeDesc> p1 = lt.date.parseOut("June 2020");
		ParseResult<TimeDesc> p2 = lt.date.parseOut("June");
		ParseResult<TimeDesc> p2b = lt.date.parseOut("March");
		ParseResult<TimeDesc> p3 = lt.date.parse("Marching Orders");
		assert p3==null;
	}			

	@Test
	public void testPrint() {
		Lang lang = new Lang();
		GrammarPrinter gp = new GrammarPrinter();
		String bnf = gp.print(LangTime.time);
		System.out.println(bnf);
	}
	
	@Test
	public void testDate() {
		Parser.clearGrammar();
		Lang lang = new Lang();
		LangTime lt = new LangTime();
		{
			lt.date.parseOut("dec");
			lt.date.parseOut("Dec");
			lt.date.parseOut("SEPTEMBER");
			lt.date.parseOut("SEPT");
			lt.date.parseOut("sep");
		}
		{
			lt.date.parseOut("Jun 2011");
			lt.date.parseOut("March 2012");
		}
	}

	@Test
	public void testQuarter() {
		Parser.clearGrammar();
		Lang lang = new Lang();
		LangTime lt = new LangTime();
		{
			ParseResult<TimeDesc> huh = lt.time.parseOut("quarter");
		}
	}

	@Test
	public void testYearAsDate() {
		Parser.clearGrammar();
		Lang lang = new Lang();
		LangTime lt = new LangTime();
		{
			lt.justYear.parseOut("2019");
		}
		{
			ParseResult<TimeDesc> pr = lt.time.parseOut("2019");
			System.out.println(pr);
		}
		{
			ParseResult<TimeDesc> pr = lt.time.parseOut("June 2019");
			System.out.println(pr);
		}
	}

	@Test
	public void testOf() {
		Parser.clearGrammar();
		Lang lang = new Lang();
		LangCellSet lcs = new LangCellSet();
		Parser rn = lcs.rowName;
		LangTime lt = new LangTime();
//		SimpleLangNum ln = new SimpleLangNum();
		Parser.DEBUG = true;
		{
			ParseResult<TimeDesc> pr = lt.time.parseOut("start of Sales");			
			TimeDesc td = pr.getX();	
			System.out.println(td);
		}
		{
			ParseResult<TimeDesc> pr = lt.time.parseOut("1 month from start of Sales");			
			TimeDesc td = pr.getX();			
		}
		if (false) {
			ParseResult<TimeDesc> pr = lt.time.parseOut("from start of Sales");			
			TimeDesc td = pr.getX();			
		}
	}

	
	@Test
	public void testComplex() {
		Parser.clearGrammar();
		Lang lang = new Lang();
		LangTime lt = new LangTime();
//		SimpleLangNum ln = new SimpleLangNum();
		{
			ParseResult<TimeDesc> pr = lt.time.parseOut("1 month from start");			
			TimeDesc td = pr.getX();
			try {
				Col col = td.getCol(null);
				assert false : col;
			} catch (Exception e) {
				// OK
				Printer.out(e);
			}
		}
		{
			ParseResult<TimeDesc> pr = lt.time.parseOut("year 1");
			TimeDesc td = pr.getX();
			Col col = td.getCol(null);
			assert col.index == 1 : col;
		}
		{
			ParseResult<TimeDesc> pr = lt.time.parseOut("year 3");
			TimeDesc td = pr.getX();
			Col col = td.getCol(null);
			assert col.index == 25 : col;
		}
		{			
			ParseResult<TimeDesc> pr = lt.time.parseOut("2 months from year 1");			
			TimeDesc td = pr.getX();
			Col col = td.getCol(null);
			assert col.index == 3 : col;
		}
		{
			ParseResult<TimeDesc> pr = lt.time.parseOut("1 year from month 2");			
			TimeDesc td = pr.getX();
			Col col = td.getCol(null);
			assert col.index == 14 : col;
		}
	}

	@Test
	public void testTime() {
		LangTime lang = new LangTime();
		lang.time.parseOut("now");
		lang.time.parseOut("month 2");
		lang.time.parseOut("start");
		lang.time.parseOut("1 year from start");
	}
	
	@Test
	public void testAgo() {
		Lang l = new Lang();
		LangTime lang = new LangTime();
		Business b = new Business();
		BusinessContext.setBusiness(b);
		ParseResult<TimeDesc> pr = lang.time.parseOut("2 months ago");
		TimeDesc td = pr.getX();
		{
			Cell bc = new Cell(null, new Col(1));
			Col c = td.getCol(bc);
			assert c== Col.THE_PAST : c;
		}
		{
			Cell bc = new Cell(null, new Col(6));
			Col c = td.getCol(bc);
			assert c.index == 4 : c;
		}
	}
	

	
	@Test
	public void testDt() {
		new SimpleLangNum();
		LangTime lt = new LangTime();
		lt.dt.parseOut("1 month");
		ParseResult<DtDesc> m2 = lt.dt.parseOut("2 months");
		Dt dt = m2.ast.getX().calculate(null);
		assert dt.equals(new Dt(2, TUnit.MONTH)) : dt;
		lt.dt.parseOut("3 years");
		
		assert lt.dt.parse("1") == null;
		assert lt.dt.parse("2 fish") == null;
	}
	
}

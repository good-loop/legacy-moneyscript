package com.winterwell.moneyscript.lang.cells;

import org.junit.Test;

import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Col;
import com.winterwell.moneyscript.lang.Lang;
import com.winterwell.moneyscript.lang.bool.LangBool;
import com.winterwell.moneyscript.lang.cells.Filter;
import com.winterwell.moneyscript.lang.cells.LangCellSet;
import com.winterwell.moneyscript.lang.cells.LangFilter;
import com.winterwell.moneyscript.lang.cells.TimeFilter;
import com.winterwell.moneyscript.lang.num.SimpleLangNum;
import com.winterwell.moneyscript.lang.time.LangTime;
import com.winterwell.moneyscript.lang.time.TimeDesc;
import com.winterwell.nlp.simpleparser.ParseResult;
import com.winterwell.nlp.simpleparser.Parser;
import com.winterwell.nlp.simpleparser.Parsers;
import com.winterwell.nlp.simpleparser.Ref;

/**
 * @tested {@link LangFilter}
 * @author daniel
 *
 */
public class LangFilterTest {

	@Test public void testAbove() {
		Parser.DEBUG = true;
		LangFilter lf = new LangFilter();
		LangFilter.filter.parseOut("above");
		
		Lang lang = new Lang();
		LangCellSet lcs = new LangCellSet();		
	
		
		
		String pabove = "cellSetAsFormula";
		Parser p = Parsers.getParser(pabove);
		
		ParseResult pr = p.parseOut("above");
		assert pr != null;
		
		Business b = lang.parse("Foo: sum above");
	}
	
	
	@Test public void testEachYear() {
		Lang lang = new Lang();
//		SimpleLangNum ln = new SimpleLangNum();
		LangFilter lf = new LangFilter();
		Parser.DEBUG = true;
		boolean all =true;
		if (all) {
			Parser ey = lf.periodicFilter;
			ParseResult pey = ey.parseOut("each year");
			System.out.println(pey);			
		}
		if (all) {
			Parser ey = lf.conditionalFilter;
			ParseResult pey = ey.parseOut("from start");
			System.out.println(pey);						
		}
		if (all) {	
			Parser ey = lf.periodicFilter;
			ParseResult pey = ey.parseOut("each year from start");
			System.out.println(pey);			
		}
		{
			Parser p = ((Ref) LangFilter.filter).lookup();
			p.parseOut("each year");
		}
		{
			LangFilter.filter.parseOut("each year");
		}
		{
			LangFilter.filter.parseOut("each year from start");
		}
//		Staff each year from start: * 110%
	}
	
	@Test
	public void testTimeFilter() {
		{
			Lang lang = new Lang();
			SimpleLangNum ln = new SimpleLangNum();
			LangFilter.filter.parseOut("at previous");
			LangFilter.filter.parseOut("at 2 months ago");
		}
	}

	@Test
	public void testFilter() {
		{
			Parser.clearGrammar();
			Lang lang = new Lang();
			SimpleLangNum ln = new SimpleLangNum();
			LangFilter.filter.parseOut("from month 2");
			LangFilter.filter.parseOut("from start");
			LangFilter.filter.parseOut("from 1 year from start");
		}
	}

	@Test
	public void testPeriod() {
		Lang lang = new Lang();
		LangTime lt = new LangTime();
		LangCellSet lcs = new LangCellSet();
		SimpleLangNum ln = new SimpleLangNum();
		{
			ParseResult<Filter> pr = LangFilter.filter.parseOut("from month 2"); 
			Filter f = pr.getX();
			assert f != null;
			TimeFilter tf = (TimeFilter) f;
		}
		{
			ParseResult<Filter> pr = LangFilter.filter.parseOut("to month 6"); 
			Filter f = pr.getX();
			assert f != null;
			TimeFilter tf = (TimeFilter) f;
		}
		{
			ParseResult<Filter> pr = LangFilter.filter.parseOut("from month 2 to month 6"); 
			Filter f = pr.getX();
			assert f != null;
		}
	}
	
	@Test
	public void testAtMonth() {
		Lang lang = new Lang();
		LangTime lt = new LangTime();
		LangCellSet lcs = new LangCellSet();
		SimpleLangNum ln = new SimpleLangNum();
		{
			lt.time.parseOut("month 1");
			ParseResult<Filter> pr = LangFilter.filter.parseOut("at month 1"); 
			Filter f = pr.getX();
			assert f != null;
			TimeFilter tf = (TimeFilter) f;
			Cell fc = new Cell(null, new Col(1));
			assert tf.contains(new Cell(null, new Col(1)), fc);
			assert ! tf.contains(new Cell(null, new Col(2)), fc);
			assert ! tf.contains(new Cell(null, new Col(3)), fc);
		}
		{
			lt.time.parseOut("month 1");
			ParseResult<Filter> pr = LangFilter.filter.parseOut("at month 3"); 
			Filter f = pr.getX();
			assert f != null;
			TimeFilter tf = (TimeFilter) f;
			Cell fc = new Cell(null, new Col(1));
			assert ! tf.contains(new Cell(null, new Col(1)), fc);
			assert ! tf.contains(new Cell(null, new Col(2)), fc);
			assert tf.contains(new Cell(null, new Col(3)), fc);
			assert ! tf.contains(new Cell(null, new Col(4)), fc);
		}
	}
}

package com.winterwell.moneyscript.lang.cells;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.winterwell.moneyscript.lang.Lang;
import com.winterwell.moneyscript.lang.Rule;
import com.winterwell.moneyscript.lang.bool.LangBool;
import com.winterwell.moneyscript.lang.cells.Filter.KDirn;
import com.winterwell.moneyscript.lang.num.Numerical;
import com.winterwell.moneyscript.lang.num.SimpleLangNum;
import com.winterwell.moneyscript.lang.num.UnaryOp;
import com.winterwell.moneyscript.lang.time.LangTime;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.BusinessContext;
import com.winterwell.moneyscript.output.BusinessState;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Col;
import com.winterwell.moneyscript.output.Row;
import com.winterwell.nlp.simpleparser.ParseResult;
import com.winterwell.nlp.simpleparser.Parser;
import com.winterwell.utils.Printer;
import com.winterwell.utils.containers.Containers;


public class LangCellSetTest {
	

	@Test public void testCellSetFrom() {
		Lang lang = new Lang();
		CellSet cs = LangCellSet.cellSet.parseOut("Staff from month 3").getX();
		assert cs instanceof FilteredCellSet;
		FilteredCellSet fcs = (FilteredCellSet) cs;
		assert fcs.base.toString().equals("Staff");
	}
	
	

	@Test 
	public void testCellSetFromConditional() {
		Lang lang = new Lang();
		Business b = lang.parse("A: 1\n"
				+"A at month 2: 10\n"
				+"Go from A > 2: 1");		
		b.setColumns(4);
		b.run();
		Rule rule = b.getRow("Go").getRules().get(0);
		FilteredCellSet filter = (FilteredCellSet) rule.getSelector();
		Cell go3 = new Cell(b.getRow("Go"), new Col(3));
		boolean in = filter.contains(go3, go3);
		assert in;
		List<Row> rows = b.getRows();
		String csv = b.toCSV();
		assert csv.contains("A, 1, 10, 1, 1");
		assert csv.contains("Go, 0, 1, 1, 1") : csv;
	}
	
	


	@Test 
	public void testCellSetFromConditionalIf() {
		Lang lang = new Lang();
		Business b = lang.parse("Staff:\n\tAlice: 2\n\tBob: 0\n"
				+"Staff from month 3 if (this row at month 1) > 1: + 1");		
		b.setColumns(4);
		b.run();
		Rule rule = b.getRow("Staff").getRules().get(0);		
		Cell go3 = new Cell(b.getRow("Alice"), new Col(3));
		List<Row> rows = b.getRows();
		String csv = b.toCSV();
		assert csv.contains("Alice, 2, 2, 3, 3") : csv;
		assert csv.contains("Bob, 0, 0, 0, 0") : csv;
	}

	

	@Test 
	public void testCellSetIfScenario() {
		Lang lang = new Lang();
		{	// test with a dumb if
			LangCellSet.cellSetFilter.parseOut("if 1 > 2");
			Business b = lang.parse("Alice if 1 > 2: 2\n");		
			b.setColumns(4);
			b.run();
			String csv = b.toCSV();
			assert csv.contains("Alice, 0, 0, 0, 0") : csv;
		}
		{
			LangCellSet.cellSetFilter.parseOut("if A > 0");
			Business b = lang.parse("scenario A:\n\nAlice if A > 0: 2\n");		
			b.setColumns(4);
			b.setScenarios(Arrays.asList("A"));
			b.run();
			String csv = b.toCSV();
			assert csv.contains("Alice, 2, 2, 2, 2") : csv;
		}
		{
			Business b = lang.parse("scenario A:\n\nAlice if A > 0: 2\n");		
			b.setColumns(4);
			b.run();
			String csv = b.toCSV();
			assert csv.contains("Alice, 0, 0, 0, 0") : csv;
		}
	}
	

	
	
	@Test public void testCellSetExcept() {
		Lang lang = new Lang();
		CellSet cs = LangCellSet.cellSet.parseOut("Staff except(Alice)").getX();
		assert cs instanceof FilteredCellSet;
		FilteredCellSet fcs = (FilteredCellSet) cs;
		assert fcs.base.toString().equals("Staff");
	}
	
	@Test
	public void testAbove() {
		Lang lang = new Lang();
		{
			lang.langFilter.dirnFilter.parseOut("above");
		}
		{
			Parser.DEBUG = true;
			lang.langFilter.filter0.parseOut("above");			
			lang.langNum.num.parseOut("sum(Alice)");
			lang.langNum.num.parseOut("sum(above)");
			lang.langNum.num.parseOut("sum above");
			Business b = lang.parse("Alice:1\nBob:2\nBoth: sum above");
		}
	}

	
	@Test
	public void testBrackets() {
		{
			Lang lang = new Lang();
			LangCellSet lcs = lang.langCellSet;			
			lcs.listOfSetsSelector.parseOut("Alice from (2 months ago)");
			lcs.listOfSetsSelector.parseOut("(Alice)");
		}
	}
	
	@Test
	public void testRuleOrdering() {
		{
			Lang lang = new Lang();
			Business b = lang.parse("A: 1\nA at month 1: 2");
			Row a = b.getRow("A");
			List<Rule> rules = a.getRules();
			assert rules.size() == 2;
			assert rules.get(0).getSelector() instanceof RowName;
			assert rules.get(1).getSelector() instanceof FilteredCellSet;
			b.setSamples(1);
			b.setColumns(3);
			b.run();
			assert a.getValues()[0] == 2;
			assert a.getValues()[1] == 1;
			assert a.getValues()[2] == 1;
		}
		{
			Lang lang = new Lang();
			Business b = lang.parse("A at month 1: 2\nA: 1");
			Row a = b.getRow("A");
			List<Rule> rules = a.getRules();
			assert rules.size() == 2;
			assert rules.get(0).getSelector() instanceof RowName;
			assert rules.get(1).getSelector() instanceof FilteredCellSet;
			b.setSamples(1);
			b.setColumns(3);
			b.run();
			assert a.getValues()[0] == 2;
			assert a.getValues()[1] == 1;
			assert a.getValues()[2] == 1;
		}
		{	// Rules with a filter beat those without
			Lang lang = new Lang();
			Business b = lang.parse("A at month 1: 2\nA: 1\nA at month 2: 3");
			Row a = b.getRow("A");
			List<Rule> rules = a.getRules();
			assert rules.size() == 3;
			assert rules.get(0).getSelector() instanceof RowName : rules;
			assert rules.get(1).getSelector() instanceof FilteredCellSet : rules;
			assert rules.get(2).getSelector() instanceof FilteredCellSet : rules;
			b.setSamples(1);
			b.setColumns(3);
			b.run();
			assert a.getValues()[0] == 2;
			assert a.getValues()[1] == 3;
			assert a.getValues()[2] == 1;
		}
	}

	
	@Test
	public void testTimeFilter_smoke() {
		Lang lang = new Lang();
		{
			lang.langFilter.filter.parseOut("at previous");
			lang.langFilter.filter.parseOut("at 2 months ago");
			lang.langFilter.filter.parseOut("from 2 months ago to month 4");
			lang.langFilter.conditionalFilter.parseOut("from 2 months ago");
			lang.langFilter.conditionalFilter.parseOut("from (2 months ago)");
			lang.langFilter.filter.parseOut("(from 2 months ago)");
			lang.langFilter.filter.parseOut("from (2 months ago) to (month 4)");
		}
		{			
			LangCellSet lcs = new LangCellSet();
			lcs.listOfSetsSelector.parseOut("Alice at previous");
			lcs.listOfSetsSelector.parseOut("Alice at 2 months ago");
			lcs.listOfSetsSelector.parseOut("Alice from month 2 to month 4");
			lcs.listOfSetsSelector.parseOut("Alice from 2 months ago to month 4");
			lcs.listOfSetsSelector.parseOut("Alice from (2 +- 1) months ago");
			lcs.listOfSetsSelector.parseOut("Alice from (2 months ago) to (month 4)");
			lcs.listOfSetsSelector.parseOut("(Alice at 2 months ago)");
		}
	}

	@Test
	public void testTimeFilterToDate() {
		Lang lang = new Lang();
		{
			lang.langFilter.filter.parseOut("to month 3");
			lang.langFilter.filter.parseOut("to date");
		}
		{			
			LangCellSet lcs = new LangCellSet();
			lcs.listOfSetsSelector.parseOut("Alice to month 3");
			lcs.listOfSetsSelector.parseOut("Alice to date");
		}
		Business b = lang.parse("Alice: 1, 2\nFoo: sum(Alice to date)");
		
		b.run();
		
		Numerical c = b.getCell(1, 2);
		assert c.doubleValue() == 3;
		
		System.out.println(b.toCSV());
		Set<Rule> rules = b.getAllRules();
		Row row = b.getRow("Foo");
		Rule rule = row.getRules().get(0);
		UnaryOp f = (UnaryOp) rule.getFormula();
		
	}

	@Test(timeout=10000)
	public void testComplex() {
		Lang lang = new Lang();
		LangTime lt = new LangTime();
		LangCellSet lcs = new LangCellSet();
		LangFilter lf = new LangFilter();
		SimpleLangNum ln = new SimpleLangNum();
		{
			ParseResult<CellSet> pr = lcs.listOfSetsSelector.parseOut("Alice from 1 month from start");
			FilteredCellSet cells = (FilteredCellSet) pr.getX();
		}
		{	// This is a bogus script -- when is start??
			Business b = lang.parse("Alice from 1 month from start: 1");
			b.setSamples(1);
			b.setColumns(3);
			b.run();
			Row alice = b.getRow("Alice");
			FilteredCellSet cells = (FilteredCellSet) alice.getRules().get(0).getSelector();
			assert Containers.same(cells.getRowNames(null), "Alice") : cells.getRowNames(null);
			TimeFilter f = (TimeFilter) cells.filter;
			// see how that goes - should fail (no start)
			Cell bc = new Cell(alice, new Col(1));
			assert ! cells.contains(new Cell(alice, new Col(1)), bc);
		}
		{
			Business b = lang.parse("Alice at month 2: 1\nAlice from 1 month from start: 2");
			b.setColumns(5);
			b.setSamples(1);
			b.run();
			Row alice = b.getRow("Alice");
			Collection<Cell> cells = alice.getCells();
			double[] vs = alice.getValues();
			assert vs[0] == 0 : Printer.toString(vs);
			assert vs[1] == 1 : Printer.toString(vs);
			assert vs[2] == 2 : Printer.toString(vs);
			assert vs[3] == 2 : Printer.toString(vs);
		}
		// This is a bogus script (when is start?)
		{
			Business b = lang.parse("Alice from 1 month from start: 2");
			b.setColumns(3);
			b.setSamples(2);
			b.run();
			Row alice = b.getRow("Alice");
			Collection<Cell> cells = alice.getCells();
			double[] vs = alice.getValues();
			for (double d : vs) {
				assert d==0 : Printer.toString(vs);
			}
		}
	}

	@Test
	public void testAtMonth() {
		Lang lang = new Lang();
		LangTime lt = new LangTime();
		LangCellSet lcs = new LangCellSet();
		LangFilter lf = new LangFilter();
		SimpleLangNum ln = new SimpleLangNum();
		{
			ParseResult<Filter> pr = lf.filter.parseOut("at month 1");
		}
		{
			ParseResult<CellSet> pr = lcs.cellSet.parseOut("Invest at month 1");
			CellSet cells = pr.getX();		
		}
		{
			Business b = lang.parse("Invest at month 1: 100");
			Row row = b.getRow("Invest");
			b.run();
			double[] vs = row.getValues();
			assert vs[0] == 100;
			assert vs[1] == 0;
			assert vs[2] == 0;
			assert row != null;
			Rule rule = row.getRules().get(0);
			FilteredCellSet fcs =  (FilteredCellSet) rule.getSelector();
			assert fcs.filter.op.equals("at") && fcs.filter.dirn == KDirn.HERE : fcs; // test for `at` 
		}
	}
	
	@Test
	public void testFromMonthEval() {
		Lang lang = new Lang();
		LangCellSet lcs = new LangCellSet();
		SimpleLangNum ln = new SimpleLangNum();
		{
			String s = "Bob from month 2: 1";
			Business b = lang.parse(s);			
			b.setSamples(1);
			b.setColumns(3);
			b.run();
			Row bob = b.getRow("Bob");
			Rule rule = bob.getRules().get(0);
			Cell b1 = new Cell(bob, new Col(1));
			Cell b2 = new Cell(bob, new Col(2));
			Cell b3 = new Cell(bob, new Col(3));
			
			CellSet bobFromM2 = rule.getSelector();			
			assert ! bobFromM2.contains(b1, b1);
			assert bobFromM2.contains(b2, b1);
			assert bobFromM2.contains(b3, b1);
			
			Cell c1 = new Cell(bob, new Col(1));
			Numerical v = rule.calculate(c1);
			assert v==null || v.doubleValue() == 0 : v;
			
			double[] bvs = b.getRow("Bob").getValues();
			Assert.assertArrayEquals(new double[]{0, 1, 1}, bvs, 0.1);
		}
	}
	
	@Test
	public void testFromMonth() {
		Lang lang = new Lang();
		LangCellSet lcs = new LangCellSet();
		SimpleLangNum ln = new SimpleLangNum();
		{		
			ParseResult<CellSet> pr = LangCellSet.cellSet.parseOut("Alice from month 2");
			CellSet cells = pr.getX();
			assert cells != null;
			assert cells instanceof FilteredCellSet : cells;
			FilteredCellSet fcs = (FilteredCellSet) cells;
			assert Containers.same(fcs.getRowNames(null), Arrays.asList("Alice")) : fcs.getRowNames(null);
			assert fcs.base instanceof RowName : fcs.base;
			assert fcs.filter instanceof TimeFilter : fcs.filter;
			
			TimeFilter tf = (TimeFilter) fcs.filter;			
			assert ! tf.contains(new Cell(null, new Col(1)), null);
			assert tf.contains(new Cell(null, new Col(2)), null);
			assert tf.contains(new Cell(null, new Col(3)), null);
			
			Business b = lang.parse("Alice from month 2: 1");
			Row alice = b.getRow("Alice");
			Col start = fcs.getStartColumn(alice, new Cell(alice, null));
			assert start.index == 2 : start;
		}
		{
			
		}
	}
	
	@Test
	public void testTimeRange_Known_BAD() {
		Lang lang = new Lang();
		{		
			ParseResult<CellSet> pr = LangCellSet.cellSet.parseOut("Alice from start to now");
			CellSet cells = pr.getX();
			assert cells != null;
		}
		{		
			ParseResult<CellSet> pr = LangCellSet.cellSet.parseOut("Alice from start of Alice to now");
			CellSet cells = pr.getX();
			assert cells != null;
		}

		// This breaks 'cos it is interpreted as base -> time filters
		// And the base evaluates to "just the one cell in the focal column".
		// Fix: evaluate base differently?? No, lets add a new keyword for "all of base"
		// e.g. row(Alice)
		{
			Business b = lang.parse("Alice: 1, 2, 3, 4");
			b.state = new BusinessState(b);
			BusinessContext.setBusiness(b);
			ParseResult<CellSet> pr = LangCellSet.cellSet.parseOut("Alice from start of Alice to now");
			CellSet cells = pr.getX();
			assert cells != null;		
			Row row = b.getRow("Alice");
			Cell c1 = new Cell(row, new Col(1));
			Cell c2 = new Cell(row, new Col(2));
			Cell c3 = new Cell(row, new Col(3));
			Collection<Cell> c1s = cells.getCells(c1, true);
			Collection<Cell> c2s = cells.getCells(c2, true);
			Collection<Cell> c3s = cells.getCells(c3, true);
			assert c1s.size() == 1 : c1s;
			assert c2s.size() == 2 : c2s;
			assert c3s.size() == 3 : c3s;
		}
	}
	
	@Test
	public void testRowName() {		
		LangCellSet lang = new LangCellSet();
		LangFilter lf = new LangFilter();
		LangTime lt = new LangTime();
		LangBool lb = new LangBool();
		SimpleLangNum ln = new SimpleLangNum();		
		{		
			lang.rowName.parseOut("Dan");
			lang.rowName.parseOut("Dan W");
			assert lang.rowName.parse("Dan from start") == null;
		}
		{
			lang.listOfSetsSelector.parseOut("Sales");
			LangFilter.filter.parseOut("to now");
			ParseResult<CellSet> pr = lang.listOfSetsSelector.parseOut("Sales to now");
			CellSet sel = pr.ast.getX();
			assert sel instanceof FilteredCellSet : sel;
		}
//		{
//			assert lang.rowName.parse("Sales to now") == null;
//			lang.formulas.mathFnUnary.parseOut("sum Sales");
//			ParseResult pr = lang.formulas.mathFnUnary.parseOut("sum Sales to now");
//		}
		{
			assert lang.rowName.parse("Sales ") == null;
			assert lang.rowName.parse("Sales sum") == null;
		}
		{	// month-like names (July, March)
			lang.rowName.parseOut("Jules");
			lang.rowName.parseOut("Marketing");
		}
		{
			assert lang.rowName.parse("Chairman (part time)") == null; //"time" is a reserved keyword
			lang.rowName.parseOut("Chairman PartTime");
		}
	}

	@Test
	public void testRowNameBad() {		
		Lang lang0 = new Lang();	
		LangCellSet lang = lang0.langCellSet;
		ParseResult<RowName> parsed = lang.rowName.parse("Foo#bar");
		assert parsed==null;		
	}
	
	@Test
	public void testSelector() {
		LangCellSet lang = new LangCellSet();
//		lang.rowName.parseOut("Alice");
//		lang.selector1.parseOut("Alice");
		lang.listOfSetsSelector.parseOut("Alice");
	}
	

	@Test public void testCellSet_rowInGroup() {
		Lang lang = new Lang();
//		Parser.DEBUG = true;
//		CellSet cs0 = LangCellSet.cellSet.parseOut("Staff if (Alice at Jan 2022) > 0").getX();
		
		CellSet cs = LangCellSet.cellSet.parseOut("Staff from Feb 2022 if (this row at Jan 2022) > 0").getX();
		assert cs instanceof FilteredCellSet;
		FilteredCellSet fcs = (FilteredCellSet) cs;
		assert fcs.base.toString().equals("Staff");
		
	}


	@Test public void testCellSet_rowInGroup_apply() {
		Lang lang = new Lang();
		Business b = lang.parse("start: Jan 2022\nend:March 2022\nStaff:\n\tAlice: 1\n"
				+"Staff from Feb 2022 if (this row at Jan 2022) > 0: * 2");
		b.run();
		String csv = b.toCSV();
		Printer.out(csv);
	}
	
	@Test public void testCellSet_rowInGroup_payriseExample() {
		Lang lang = new Lang();
		{
			Business b = lang.parse("start: Jan 2022\nend:June 2022\nStaff:\n\tAlice: 1\n\tBob from March 2022: 1\n"
					+"Staff from Feb 2022 if (this row at Jan 2022) > 0: * 2");
			b.run();
			String csv = b.toCSV();
			Printer.out(csv);
		}
		{
			Business b = lang.parse("start: Jan 2022\nend:June 2022\nStaff:\n\tAlice: 1\n\tBob from March 2022: 1\n"
					+"Staff from July 2022 if (this row at Jan 2022) > 0: * 104% // £5k per year // some rumblings\n"				
					);
			b.run();
			String csv = b.toCSV();
			Printer.out(csv);
		}
	}

	@Test
	public void testCurrentRow() {
		{
			Lang lang = new Lang();
			Business b = lang.parse("Alice: row\nBob: £1");
			Row alice = b.getRow("Alice");
			Row bob = b.getRow("Bob");
			Rule rule = alice.getRules().get(0);
			CellSet sel = rule.getSelector();
			Cell bc = new Cell(alice,null);
			Col col = new Col(3);
			assert sel.contains(new Cell(alice, col), bc);
			assert ! sel.contains(new Cell(bob, col), bc);
		}
		{
			Lang lang = new Lang();
			Business b = lang.parse("Alice: £1\nAlice from month 2: * 2\nBob: £1");
			Row alice = b.getRow("Alice");
			Row bob = b.getRow("Bob");
			Rule rule1 = alice.getRules().get(0);
			Rule rule2 = alice.getRules().get(1);
			CellSet sel = rule2.getSelector();
			Cell bc = new Cell(alice, null);
			Col col = new Col(3);
			assert sel.contains(new Cell(alice, col), bc);
			assert ! sel.contains(new Cell(bob, col), bc);
		}
	}
}

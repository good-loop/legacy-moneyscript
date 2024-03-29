package com.winterwell.moneyscript.lang;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

import com.winterwell.moneyscript.lang.cells.CellSet;
import com.winterwell.moneyscript.lang.cells.FilteredCellSet;
import com.winterwell.moneyscript.lang.cells.LangCellSet;
import com.winterwell.moneyscript.lang.cells.Scenario;
import com.winterwell.moneyscript.lang.cells.SimpleLangCellSet;
import com.winterwell.moneyscript.lang.num.BasicFormula;
import com.winterwell.moneyscript.lang.num.ColVar;
import com.winterwell.moneyscript.lang.num.Formula;
import com.winterwell.moneyscript.lang.num.LangNum;
import com.winterwell.moneyscript.lang.num.Numerical;
import com.winterwell.moneyscript.lang.num.SimpleLangNum;
import com.winterwell.moneyscript.lang.time.RelativeTimeDesc;
import com.winterwell.moneyscript.lang.time.TimeDesc;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.BusinessContext;
import com.winterwell.moneyscript.output.BusinessState;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Col;
import com.winterwell.moneyscript.output.Row;
import com.winterwell.nlp.simpleparser.GrammarPrinter;
import com.winterwell.nlp.simpleparser.ParseResult;
import com.winterwell.nlp.simpleparser.Parser;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.containers.Slice;
import com.winterwell.utils.containers.Tree;
import com.winterwell.utils.io.FileUtils;


public class LangTest {

	@Test
	public void testSplitBy() {
		Lang lang = new Lang();
		{
			//lang.langCellSet.selector1.parseOut("Cost split by Staff");
			LangCellSet.cellSet.parseOut("Cost split by Staff");
			ParseResult<Rule> pr = lang.rule.parseOut("Cost split by Staff: Staff#name");
		}
		{
			Business b = lang.parse("start: Jan 2020\nend: Jan 2020\nStaff: #name\n\tAlice: 1\n\tBob: 2\n"
					+"Cost split by Staff: Staff#name");
			b.run();
			String csv = b.toCSV();
			assert csv.contains("Staff, 3") : csv;
			assert csv.contains("Alice, 1") : csv;
			assert csv.contains("Cost for Alice, 1") : csv;
		}
	}
	
	@Test
	public void testGroupWithFilter() {
		Lang lang = new Lang();
		{
			Business b = lang.parse("start: Jan 2021\nStaff from Jan 2022:\n\tAlice: 1");
			Row row = b.getRow("Alice");
			List<Rule> rules = row.getRules();
			assert rules.size() == 1;
			Rule rule = rules.get(0);
			assert rule.getSelector() instanceof FilteredCellSet;
			b.setColumns(3);
			b.run();
			String csv = b.toCSV();
			assert csv.contains("Staff, 0, 0") : csv;
			assert csv.contains("Alice, 0, 0") : csv;
		}
		{
			Business b = lang.parse("start: Dec 2021\nStaff from Jan 2022:\n\tAlice: 1");
			Row row = b.getRow("Alice");
			List<Rule> rules = row.getRules();
			assert rules.size() == 1;
			Rule rule = rules.get(0);
			assert rule.getSelector() instanceof FilteredCellSet;
			b.setColumns(3);
			b.run();
			String csv = b.toCSV();
			assert csv.contains("Staff, 0, 1") : csv;
			assert csv.contains("Alice, 0, 1") : csv;
		}
	}


	@Test
	public void testNestedGroupWithFilter() {
		Lang lang = new Lang();
		{
			Business b = lang.parse("start: Jan 2021\nStaff from Jan 2022:\n\tUK:\n\t\tAlice: 1");
			Row rowuk = b.getRow("UK");
			List<Rule> rulesuk = rowuk.getRules();
			assert rulesuk.size() == 1;
			Rule ruleuk = rulesuk.get(0);
			assert ruleuk.getSelector() instanceof FilteredCellSet : ruleuk.getSelector();
			
			Row row = b.getRow("Alice");
			List<Rule> rules = row.getRules();
			assert rules.size() == 1;
			Rule rule = rules.get(0);
			assert rule.getSelector() instanceof FilteredCellSet : rule.getSelector();
			b.setColumns(3);
			b.run();
			String csv = b.toCSV();
			assert csv.contains("Staff, 0, 0") : csv;
			assert csv.contains("Alice, 0, 0") : csv;
		}
		{
			Business b = lang.parse("start: Jan 2021\nStaff from Jan 2022:\n\tUK:\n\t\tSales:\n\t\t\tAlice: 1");
			Row rowuk = b.getRow("UK");
			List<Rule> rulesuk = rowuk.getRules();
			assert rulesuk.size() == 1;
			Rule ruleuk = rulesuk.get(0);
			assert ruleuk.getSelector() instanceof FilteredCellSet : ruleuk.getSelector();
			
			Row row = b.getRow("Alice");
			List<Rule> rules = row.getRules();
			assert rules.size() == 1;
			Rule rule = rules.get(0);
			assert rule.getSelector() instanceof FilteredCellSet : rule.getSelector();
			b.setColumns(3);
			b.run();
			String csv = b.toCSV();
			assert csv.contains("Staff, 0, 0") : csv;
			assert csv.contains("Alice, 0, 0") : csv;
		}
	}

	@Test @Ignore // currently fails :( Workaround: don't mix grouping with style rules
	public void testNestedGroupWithFilterAndStyle() {
		Lang lang = new Lang();
		{
			Business b = lang.parse("start: Jan 2021\nStaff from Jan 2022:\n\tUK: {.bg-blue}\n\t\tAlice: 1");
			Row rowuk = b.getRow("UK");
			List<Rule> rulesuk = rowuk.getRules();
			assert rulesuk.size() == 1 : rulesuk;
			Rule ruleuk = rulesuk.get(0);
			assert ruleuk.getSelector() instanceof FilteredCellSet : ruleuk.getSelector();
			
			Row row = b.getRow("Alice");
			List<Rule> rules = row.getRules();
			assert rules.size() == 1;
			Rule rule = rules.get(0);
			assert rule.getSelector() instanceof FilteredCellSet : rule.getSelector();
			b.setColumns(3);
			b.run();
			String csv = b.toCSV();
			assert csv.contains("Staff, 0, 0") : csv;
			assert csv.contains("Alice, 0, 0") : csv;
		}
	}
	
	@Test
	public void testTagMaths() {
		Lang lang = new Lang();
		{	// no effect on normal sums
			Business b = lang.parse("Staff:\n\tAlice: 1 #amber\n\tBob: 2 #blue\n"
					+"Overheads: Staff * 10%");
			b.setColumns(3);
			b.run();
			String csv = b.toCSV();
			assert csv.contains("Staff, 3, 3") : csv;
			assert csv.contains("Overheads, 0.3") : csv;
		}
		{
			Business b = lang.parse("Staff:\n\tAlice: 1 #amber\n\tBob: 2 #blue\n"
					+"Overheads: Staff * 10%\n"
					+"YellowStaff: Staff#amber\n");
			b.setColumns(3);
			b.run();
			String csv = b.toCSV();
			System.out.println(csv);
			assert csv.contains("Staff, 3, 3") : csv;
			assert csv.contains("Overheads, 0.3") : csv;
			assert csv.contains("YellowStaff, 1, 1") : csv;
		}
		{
			Business b = lang.parse("Staff:\n\tAlice: 1 #amber\n\tBob: 2 #blue\n"
					+"Overheads: Staff * 10%\n"
					+"OpEx: Staff + Overheads\n"
					+"BlueOpEx: OpEx#blue");
			b.setColumns(3);
			b.run();
			String csv = b.toCSV();
			System.out.println(csv);
			assert csv.contains("Staff, 3, 3") : csv;
			assert csv.contains("Overheads, 0.3") : csv;
			assert csv.contains("BlueOpEx, 2.2, 2.2") : csv;
		}
	}
	
	@Test
	public void testPossibleStuckBug() {
		Lang lang = new Lang();
		Business b = new Business();
		Rule ok = lang.parseLine("	Sales_2021: sum(Sales Revenue from Jan 2021 to Dec 2021)", b);
		System.out.println(ok);
	}
    
	@Test
	public void testEgs() {
		// enable the eg() tests 
		Parser.DEBUG = true;
		// build Lang inc egs
		Lang lang = new Lang();
	}
	
	@Test
	public void testPrintToBNF() {
		Lang lang = new Lang();
		GrammarPrinter gp = new GrammarPrinter();
		gp.useOrMarkForFirst = true;
		gp.useQMarkForOptional = false;
		CharSequence page = gp.print(lang.line);
		FileUtils.write(new File("grammar_bnf.txt"), page);
	}

	@Test
	public void testListValues2() {
		Lang lang = new Lang();
		lang.langNum.numList.parseOut("1, 2");
		Parser.DEBUG = true;
		lang.ruleBody.parseOut("1, 2");
		lang.rule.parseOut("A: 1, 2");
		{
			Business b = lang.parse("Alice: 1, 2, 3");
			Row alice = b.getRow("Alice");
			ListValuesRule rule = (ListValuesRule) alice.getRules().get(0);
			Col col1 = new Col(1);
			Col col2 = new Col(2);
			Col col4 = new Col(4);
			Numerical v1 = rule.calculate(new Cell(alice, col1));
			Numerical v2 = rule.calculate(new Cell(alice, col2));
			Numerical v3 = rule.calculate(new Cell(alice, col4));
			assert v1.doubleValue() == 1;
			assert v2.doubleValue() == 2;
			assert v3==null || v3.doubleValue() == 0;
		}
	}


	@Test public void testExcept() {
		String plan = "Staff:\n\tAlice: £12k per year\n\tBob: £12k per year\n"
					+"Staff from month 3: + £12k per year";
		Lang lang = new Lang();
		Business b = lang.parse(plan);
		b.setColumns(6);
		b.run();
		
		List<Row> rows = b.getRows();
		String srows = Printer.str(rows);
		assert srows.equals("[Staff, Alice, Bob]") : srows;
	}
	
	@Test
	public void testSimpleRule() {
		Lang lang = new Lang();
		{
			lang.langCellSet.rowName.parseOut("Alice");
			lang.langNum.num.parseOut("1");
			lang.line.parseOut("Alice: £1");
			lang.line.parseOut("Alice: £1 per month");
		}
		{
			ParseResult one = lang.ruleBody.parseOut("1");
			BasicFormula f1 = (BasicFormula) one.ast.getX();
			assert f1 != null && f1.calculate(null).doubleValue() == 1 : f1;
			ParseResult row = lang.ruleBody.parseOut("row");
			Formula fVar = (Formula) row.ast.getX();			
			
			ParseResult<Rule> r1 = lang.rule.parseOut("Alice: 1");
			ParseResult<Rule> r2 = lang.rule.parseOut("Alice: row");
			Rule rule1 = r1.getX(lang.rule);
			Rule rule2 = r2.getX(lang.rule);
			assert ! (rule1 instanceof DummyRule) : rule1;
			assert rule1.formula instanceof BasicFormula : rule1.formula;
			assert rule2.formula.getClass() == ColVar.class : rule1.formula;
		}
	}
	
	@Test
	public void testStart() {
		Lang lang = new Lang();
//		if (false) 
		{
			Business b = lang.parse("Alice from month 2: £1 per month");
			b.setColumns(3);
			b.setSamples(1);
			b.run();			

			Row alice = b.getRow("Alice");
			double[] vals = alice.getValues();
			assert Arrays.equals(vals, new double[] {0, 1, 1}) : Printer.toString(vals);
			
			Numerical cv = b.getCellValue(new Cell(alice, new Col(1)));
			assert cv==null || cv.doubleValue() == 0 : cv;
			Numerical cv2 = b.getCellValue(new Cell(alice, new Col(2)));
			assert cv2.doubleValue() == 1 : cv;
			TimeDesc td = new RelativeTimeDesc("start");
			
			Cell bc = new Cell(alice, null);
			Col col = td.getCol(bc);
			assert col.index == 2 : col;
		}
		{
			Business b = lang.parse("Alice: £1 per month\nAlice from start: + £2");
			b.run();
			Row alice = b.getRow("Alice");
			Rule r2 = alice.getRules().get(1);
			Cell context = new Cell(alice, new Col(1));
			Col start = r2.getSelector().getStartColumn(alice, context);
			assert start.index == 1 : start;
			for(Cell cell : alice.getCells()) {
				System.out.print(b.getCellValue(cell)+"\t");
			}
			Numerical v2 = b.getCellValue(new Cell(alice, new Col(1)));
			assert v2.doubleValue() == 3 : v2;
		}
		{	// simple version of the next test
			Business b = lang.parse("Alice from month 2: £1 per month\n");
			b.run();
			Row alice = b.getRow("Alice");
			Collection<Cell> cells = alice.getCells();
			double[] vals = alice.getValues();
			Rule r2 = alice.getRules().get(0);
			Cell context = new Cell(alice, new Col(1));
			Col start = r2.getSelector().getStartColumn(alice, context);
			assert start.index == 2 : start;
		}
		// A tricky example: we try to calculate start whilst evaluating
		// When evaluating col(1), what should start do?
		// We will take eval==0
		{
			Business b = lang.parse("Alice from month 2: £1 per month\n"
									+"Alice from 1 month from start: + £2");
//			b.setColumns(3);
//			b.setSamples(1);
			b.run();
			Row alice = b.getRow("Alice");
			Collection<Cell> cells = alice.getCells();
			double[] vals = alice.getValues();
			// £1 from month 2
			Rule r1 = alice.getRules().get(0);
			Cell bc = new Cell(alice, null);
			Col start1 = r1.getSelector().getStartColumn(alice, bc);
			assert start1.index == 2 : start1;
			// 1+2 from 1 month from start
			Rule r2 = alice.getRules().get(1);
			Col start = r2.getSelector().getStartColumn(alice, bc);
			assert start.index == 3 : start;
			for(Cell cell : alice.getCells()) {
				System.out.print(b.getCellValue(cell)+"\t");
			}
			Numerical v2 = b.getCellValue(new Cell(alice, new Col(3)));
			assert v2.doubleValue() == 3 : v2;
		}
	}
	
	@Test public void testCompoundRule() {
		Lang lang = new Lang();
		SimpleLangNum ln =  new SimpleLangNum();
		LangCellSet lcs = new SimpleLangCellSet();
		{
			ParseResult pr0 = lang.ruleBody.parseOut("+ 10 per month");
			Object f = pr0.getX();
			assert f instanceof Formula : f;
			lang.rule.parseOut("Alice: 10");
			ParseResult<Rule> pr1 = lang.rule.parseOut("Alice: + 10");
			Rule r1 = pr1.getX();
			assert r1 != null : pr1;
			assert ! (r1 instanceof DummyRule) : r1;
		}
		new LangNum();
		lcs = new LangCellSet();
		{
			Business b = lang.parse("Alice:£1 per month\nAlice from month 3: + £10 per month");
			b.run();
			Row alice = b.getRow("Alice");
			List<Rule> rules = alice.getRules();
			assert rules.size() == 2 : rules;
			Numerical v2 = b.getCellValue(new Cell(alice, new Col(2)));
			assert v2.doubleValue() == 1 : v2;
			Col c3 = new Col(3);			
			Numerical v3 = b.getCellValue(new Cell(alice, c3));
			assert v3.doubleValue() == 11 : v3;
			for(Cell cell : alice.getCells()) {
				System.out.print(b.getCellValue(cell)+"\t");
			}
		}
		if (false) {	// TODO a compound without a non-compound start
			// -- should this work??
			Business b = lang.parse("Alice from month 3: + £10 per month");
			b.run();
			Row alice = b.getRow("Alice");
			assert alice != null;
			List<Rule> rules = alice.getRules();
			assert rules.size() == 1 : rules;
			Col c3 = new Col(3);
			Numerical cv = b.getCellValue(new Cell(alice, c3));
			assert cv.doubleValue() == 10 : cv;
			for(Cell cell : alice.getCells()) {
				System.out.print(b.getCellValue(cell)+"\t");
			}
		}
	}

	
	@Test
	public void testGroupRule() {
		Lang lang = new Lang();
		{
			lang.rule.parseOut("Staff from month 3: £10");
		}
		{			
			Business b = lang.parse("Staff:\n" +
					"\tAlice:£1 per month\n" +
					"\tBob:£1 per month\n" +
					"Staff from month 3: + £10 per month");
			b.run();
			Row alice = b.getRow("Alice");
			double[] aliceVals = alice.getValues();
			List<Rule> rules = alice.getRules();
			assert rules.size() == 2 : rules;
			Col c3 = new Col(3);
			Numerical v3 = alice.calculate(c3, b);
			assert v3.doubleValue() == 11 : Printer.toString(aliceVals);
			for(Cell cell : alice.getCells()) {
				System.out.print(b.getCellValue(cell)+"\t");
			}
		}		
	}
	

	@Test
	public void testGroupRuleHashTag() {
		Lang lang = new Lang();
		{
			lang.groupRow.parseOut("UK: #foo");
			lang.groupRow.parseOut("UK: #foo");
			lang.groupRow.parseOut("	UK: #foo");
		}
		{			
			Business b = lang.parse("Staff: #uk\n" +
					"\tAlice:£1 per month\n" +
					"\tBob:£1 per month\n" +
					"SUK: Staff#uk");
			b.setColumns(3);
			b.run();
//			System.out.println(b.toCSV());
			assert b.toCSV().contains("SUK, £2, £2");
		}
		{	// FIXME rule caching means group-level hashtags are sticky :(
			Business b = lang.parse("Staff:\n" +
					"\tAlice:£1 per month #uk\n" +
					"\tBob:£1 per month\n" +
					"SUK: Staff#uk");
			b.setColumns(2);
			b.run();
//			System.out.println(b.toCSV());
			assert b.toCSV().contains("SUK, £1, £1");
		}		
		{			
			Business b = lang.parse("Staff:\n\tLondon: #uk\n" +
					"\t\tAlice:£1 per month\n" +
					"\tBob:£1 per month\n" +
					"SUK: Staff#uk");
			b.setColumns(3);
			b.run();
//			System.out.println(b.toCSV());
			assert b.toCSV().contains("SUK, £1, £1");
		}		
	}
	
	@Test
	public void testScenarioRule() {
		Lang lang = new Lang();
		ParseResult<Rule> sa = lang.groupRow.parseOut("scenario A:");
		GroupRule gr = (GroupRule) sa.getX();
		assert gr.getScenario().equiv("A") : gr.getScenario();
		{	// scenario off
			Business b = lang.parse(
					"start: Jan 2020\nAlice:£1 per month\n" +
					"scenario Growth:\n" +
					"\tBob from month 2: £2 per month");
			b.run();
			Row alice = b.getRow("Alice");
			Row bob = b.getRow("Bob");
			Rule br = bob.getRules().get(0);
			assert br.getScenario().equiv("Growth") : br;
			Col c3 = new Col(3);
			Numerical v3 = bob.calculate(c3, b);
			assert Numerical.isZero(v3) : v3;
			String csv = b.toCSV();
			Printer.out(csv);
			double[] bobVals = bob.getValues();
			assert MathUtils.sum(bobVals) == 0;
		}
		{	// scenario on
			Business b = lang.parse(
					"Alice:£1 per month\n" +
					"scenario Growth:\n" +
					"\tBob from month 2: £2 per month");
			b.setScenarios(new ArrayMap(new Scenario("Growth"), true));
			b.run();
			Row bob = b.getRow("Bob");
			Rule br = bob.getRules().get(0);
			assert br.getScenario().equiv("Growth") : br;
			Col c3 = new Col(3);
			Numerical v3 = bob.calculate(c3, b);
			assert v3.doubleValue() == 2 : v3;
			String csv = b.toCSV();
			Printer.out(csv);
			double[] bobVals = bob.getValues();
			assert MathUtils.sum(bobVals) > 2;
		}
		if (false) {	// TODO scenario on by default
			Business b = lang.parse(
					"Alice:£1 per month\n" +
					"scenario(on) Growth:\n" +
					"\tBob from month 2: £2 per month");
			Map<Scenario, Boolean> scs = b.getScenarios();
			assert scs.containsKey(new Scenario("Growth")) : scs; 
			b.run();
			Row bob = b.getRow("Bob");
			Rule br = bob.getRules().get(0);
			assert br.getScenario().equiv("Growth") : br;
			Col c3 = new Col(3);
			Numerical v3 = bob.calculate(c3, b);
			assert v3.doubleValue() == 2 : v3;
			String csv = b.toCSV();
			Printer.out(csv);
			double[] bobVals = bob.getValues();
			assert MathUtils.sum(bobVals) > 2;
		}		
	}
	
	

	@Test
	public void testScenarioAsFilter() {
		Lang lang = new Lang();
		ParseResult<Rule> sa = lang.groupRow.parseOut("scenario A:");
		GroupRule gr = (GroupRule) sa.getX();
		assert gr.getScenario().equiv("A") : gr.getScenario();
		{	// scenario off
			Business b = lang.parse(
					"start: Jan 2020\nAlice if B == 0: £1\n" +
					"scenario B:\n" +
					"\tBob: £2");
			b.run();
			Row alice = b.getRow("Alice");
			Row bob = b.getRow("Bob");
			Rule br = bob.getRules().get(0);
			String csv = b.toCSV();
			System.out.println(csv);
			assert csv.contains("Alice, £1, £1");
			assert csv.contains("Bob, 0, 0");
		}
		{	// scenario on
			Business b = lang.parse(
					"start: Jan 2020\nAlice if B == 0: £1\n" +
					"scenario B:\n" +
					"\tBob: £2");
			b.setScenarios(Arrays.asList(new Scenario("B")));
			b.run();
			Row alice = b.getRow("Alice");
			Row bob = b.getRow("Bob");
			Rule br = bob.getRules().get(0);
			String csv = b.toCSV();
			System.out.println(csv);
			assert csv.contains("Alice, 0, 0");
			assert csv.contains("Bob, £2, £2");
		}
	}
	
	
	public void testGroupRuleSum_Known_BAD() {
		Lang lang = new Lang();
		{
			Parser.DEBUG = false;
			lang.rule.parseOut("Staff from 1 year from start: £10");
			Business b = lang.parse("Staff:\n"
					+"\tAlice from month 2:£1 per month\nStaff from 1 month from start: + £5 per month");
			Row alice = b.getRow("Alice");
			
			b.run();
			for(Cell cell : alice.getCells()) {				
				System.out.print(b.getCellValue(cell)+"\t");				
			}
			System.out.println();			
			
			CellSet aliceFromM2 = alice.getRules().get(0).getSelector();
			Cell bc = new Cell(alice, new Col(1));
			assert aliceFromM2.getStartColumn(alice, bc).index == 2 : aliceFromM2.getStartColumn(alice, bc);
			
			Rule fromStartRule = alice.getRules().get(1);
			CellSet fromStartPlus1 = fromStartRule.getSelector();
			assert ! fromStartPlus1.contains(new Cell(alice, new Col(1)), bc);
			assert ! fromStartPlus1.contains(new Cell(alice, new Col(2)), bc); // Alice doesn't have a non-zero until 2, so start=2
			assert fromStartPlus1.contains(new Cell(alice, new Col(3)), bc);
			Col start = fromStartRule.getSelector().getStartColumn(alice, bc);
			assert start.index == 3: start;
			
			Col c3 = new Col(2);
			Numerical v3a = b.getCellValue(new Cell(alice, c3));
			Numerical v3 = alice.calculate(c3, b);
			assert v3.doubleValue() == v3a.doubleValue() : v3a+" != "+v3;
			Col c4 = new Col(3);
			Numerical v4 = alice.calculate(c4, b);

			assert v3.doubleValue() == 1 : v3;
			assert v4.doubleValue() == 6 : v4;
			
		}
	}

	
	@Test
	public void testParse() {
		Lang lang = new Lang();
		Business b = lang.parse("Staff:\nAlice: £1k per month\nBob:£1k per month\nOverheads: 20% * Staff");
		Printer.out(b);
	}


	@Test
	public void testCssRule() {		
		Lang lang = new Lang();
		{
			ParseResult<Rule> pr = lang.rule.parseOut("Alice: {font-size:150%}");
			Rule rule = pr.getX();
			assert rule instanceof StyleRule : rule;
			StyleRule r = (StyleRule) rule;
			assert "font-size:150%".equals(r.getCSS());
		}
		{
			assert lang.langMisc.css.parse("{font-size:150%") == null;
			assert lang.langMisc.css.parse("font-size:150%}") == null;
			assert lang.langMisc.css.parse("{font-size:150%}") != null;
		}
	}

	@Test
	public void testMetaRule() {		
		Lang lang = new Lang();
		{
			ParseResult<Rule> pr = lang.rule.parseOut("Alice, Bob: plot");
			Rule rule = pr.getX();
			assert rule instanceof MetaRule : rule;
			MetaRule r = (MetaRule) rule;
			assert "plot".equals(r.meta);
		}
		{
			assert lang.langMisc.meta.parse("plotting") == null;
			assert lang.langMisc.meta.parse("Plot") == null;
			assert lang.langMisc.meta.parse("plot") != null;
		}
	}

	
	@Test
	public void testPlan_Known_DODGY() {
		Parser.DEBUG = false;
		String txt = FileUtils.read(new File("plans/business-plan.txt"));
		String[] lines = StrUtils.splitLines(txt);
		Lang lang = new Lang();
		for (String line : lines) {
			if (Utils.isBlank(line)) {
				Printer.out("");
				continue;
			}
			ParseResult pr = lang.line.parse(line);
			if (pr==null) {
				Printer.out("FAIL: "+line);
				continue;
			}
			Collection<Tree<Slice>> leaves = pr.ast.getLeaves();			
			Printer.out("PASS: "+StrUtils.join(leaves, " "));
			Object rule = pr.ast.getX();
			if (rule==null) Printer.out("	but no cigar");
			else Printer.out("	as "+rule);
		}				
	}
	

	@Test
	public void testGLPlan() {
		Parser.DEBUG = false;
		String txt = FileUtils.read(new File("test/gl.txt"));
		String[] lines = StrUtils.splitLines(txt);
		Lang lang = new Lang();
		for (String line : lines) {
			if (Utils.isBlank(line)) {
				Printer.out("");
				continue;
			}
			ParseResult pr = lang.line.parse(line);
			if (pr==null) {
				Printer.out("FAIL: "+line);
				continue;
			}
			Collection<Tree<Slice>> leaves = pr.ast.getLeaves();			
			Printer.out("PASS: "+StrUtils.join(leaves, " "));
			Object rule = pr.ast.getX();
			if (rule==null) Printer.out("	but no cigar");
			else Printer.out("	as "+rule);
		}				
	}

	@Test
	public void testGroupRowsIgnoringComments() {
		{	// no comment
			Lang lang = new Lang();
			Business b = lang.parse("Staff:\n\tAlice:£1 per month\n\tBob:£2 per month\n");
			BusinessContext.setBusiness(b);
			Row row = b.getRow("Staff");
			assert row.isGroup();
			GroupRule gr = (GroupRule) row.getRules().get(0);
			assert gr.indent == 0;
			
			Row alice = b.getRow("Alice");
			Rule ar = alice.getRules().get(0);
			assert ar.indent == 1;
			
			Row bob = b.getRow("Bob");
			assert bob.getParent() == row : bob.getParent();
		}
		{	// unaligned comment
			Lang lang = new Lang();
			Business b = lang.parse("Staff:\n\tAlice:£1 per month\n//Ignore me\n\tBob:£2 per month\n");
			BusinessContext.setBusiness(b);
			Row row = b.getRow("Staff");
			assert row.isGroup();
			GroupRule gr = (GroupRule) row.getRules().get(0);
			assert gr.indent == 0;
			
			Row alice = b.getRow("Alice");
			Rule ar = alice.getRules().get(0);
			assert ar.indent == 1;
			
			Row bob = b.getRow("Bob");
			assert bob.getParent() == row : bob.getParent();
		}
	}
	
	@Test
	public void testGroupRows() {
		Lang lang = new Lang();
		Business b = lang.parse("Staff:\n\tAlice:£1 per month\n\tBob:£1 per month\nOverheads: 0.1 * Staff");
		BusinessContext.setBusiness(b);
		Row row = b.getRow("Staff");
		GroupRule gr = (GroupRule) row.getRules().get(0);
		assert gr.indent == 0;
		
		Row alice = b.getRow("Alice");
		Rule ar = alice.getRules().get(0);
		assert ar.indent == 1;
		
		Row bob = b.getRow("Bob");
		
		Row overheads = b.getRow("Overheads");
		Rule oh = overheads.getRules().get(0);
		assert oh.indent == 0;
		assert oh.formula != null;
		
		List<Row> subs = row.getChildren();
		assert subs.size() == 2 : subs;
		assert subs.contains(alice);
		assert ! subs.contains(overheads);
		
		b.state = new BusinessState(b);
		
		BusinessContext.setActiveRule(oh);
		Col col = new Col(1);
		Cell cell = new Cell(overheads, col);
		b.put(new Cell(alice, col), new Numerical(1, "£"));
		b.put(new Cell(bob, col), new Numerical(1, "£"));		
		Numerical v0 = oh.formula.calculate(cell);
		Numerical v = overheads.calculate(col, b);
		assert v.doubleValue() == v0.doubleValue();
		assert v.doubleValue() == 0.2 : v;
	}

	
	@Test
	public void testGroupRows2() {
		Lang lang = new Lang();
		Business b = lang.parse("Staff:\n\tAlice:£1\nOverheads:Staff*10%\nStaff:\n\tBob:£2");
		List<Row> rows = b.getRows();
		List<String> rowNames = Containers.apply(rows, Row::getName);
		Printer.out(rows);
		assert rows.size() == 4;		
		assert Containers.same(rowNames, Arrays.asList("Staff Alice Bob Overheads".split(" ")));
	}
	
//	@Test TODO scenarios
	public void testGroupByScenario() {
		Lang lang = new Lang();
		{
			ParseResult<Rule> pr = lang.groupRow.parseOut("scenario A:");
			GroupRule gr = (GroupRule) pr.getX();
		}
		{
			Business b = lang.parse("scenario A:\n\tAlice:£1\n\n"
									+"scenario B:\n\tAlice:£2\n");
			BusinessContext.setBusiness(b);
			Row alice = b.getRow("Alice");
			List<Rule> rules = alice.getRules();
			assert rules.size() == 2;
			assert "A".equals(rules.get(0).getScenario()) : rules.get(0).getScenario();  
			assert "B".equals(rules.get(1).getScenario()) : rules.get(1).getScenario();
		}
	}
	
	@Test
	public void testRunPlan() {
		Parser.DEBUG = false;
		String txt = FileUtils.read(new File("plans/business-plan.txt"));
		Lang lang = new Lang();
		Business b = lang.parse(txt);
		b.run();
		Printer.out(b.toString());
		Printer.out(b.toCSV());
	}

	
	@Test
	public void testOneTimeNum() {
		Lang lang = new Lang();
		Business b = lang.parse("Invest at month 1: £400k");
		Row inv = b.getRow("Invest");
		Rule rule = inv.getRules().get(0);
		BusinessContext.setActiveRule(rule);
		Formula f = rule.formula;
		Col c0 = new Col(1);
		Numerical v0 = f.calculate(new Cell(inv, c0));
		Col c1 = new Col(2);
		Numerical v1 = rule.calculate(new Cell(inv, c1));
		assert v0.doubleValue() == 400000 : v0;
		assert v1==null || v1.doubleValue() == 0 : v1;
	}

	

	@Test
	public void testRefRule() {
		Lang lang = new Lang();
		lang.rule.parseOut("Bob: Alice");
//		lang.rule.parseOut("Bob:= Alice");
//		lang.rule.parseOut("Bob: = Alice");
	}
	
	
	
	
	@Test
	public void testParsing() {
		Lang lang = new Lang();
		lang.langCellSet.rowName.parseOut("Worker");
		lang.langNum.num.parseOut("-£10");
		lang.langNum.num.parseOut("-£10 per month");
		lang.langNum.num.parseOut("-£10 per month");
		lang.langCellSet.cellSet.parseOut("Alice");
		lang.langNum.num.parseOut("10");
		lang.rule.parseOut("Alice:10");
		lang.rule.parseOut("Worker: -£10");
		ParseResult pr = lang.rule.parseOut("Worker: -£10 per month");				
		assert pr != null;
		Printer.out(pr);		
		
		lang.rule.parseOut("Worker: -£10 * 100");
	}
}

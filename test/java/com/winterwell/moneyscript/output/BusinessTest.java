package com.winterwell.moneyscript.output;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.winterwell.maths.stats.distributions.d1.IDistribution1D;
import com.winterwell.moneyscript.lang.Lang;
import com.winterwell.moneyscript.lang.Rule;
import com.winterwell.moneyscript.lang.UncertainNumerical;
import com.winterwell.moneyscript.lang.cells.RowName;
import com.winterwell.moneyscript.lang.num.Formula;
import com.winterwell.moneyscript.lang.num.Numerical;
import com.winterwell.nlp.simpleparser.Parser;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.time.Time;

public class BusinessTest {
	@Test
	public void testStartEnd1Year() {
		{	// year totals - saving pocketmoney
			String s = "start: Jan 2020\nend: Dec 2020\nAlice: previous + £1";
			Lang lang = new Lang();
			Business b = lang.parse(s);
						
			List<Col> cols = b.getColumns();
			Col first = cols.get(0);
			Col last = cols.get(cols.size()-1);
			
			Time firstt = first.getTime();
			Time lastt = last.getTime();
			assert firstt.getMonth() == 1;
			assert lastt.getMonth() == 12;
			
			Time dec = new Time(2020,12,01);
			Col col = b.getColForTime(dec);
			assert col.equals(last);
		}
	}

	@Test
	public void testDeltaPrevious() {
		{
			String s = "Alice: previous + £1\nAliceDelta: previous(Alice) - Alice";
			Lang lang = new Lang();
			Business b = lang.parse(s);
			b.setSamples(1);
			b.setColumns(4);
			b.run();
			double[] bvs = b.getRow("Alice").getValues();
			Assert.assertArrayEquals(bvs, new double[]{1, 2, 3, 4}, 0.1);
			
			double[] delta = b.getRow("AliceDelta").getValues();
			Printer.out(b.toCSV());
			Assert.assertArrayEquals(delta, new double[]{-1, -1, -1, -1}, 0.1);
		}
	}
	
	@Test
	public void test2MAgo() {
		{
			String s = "Alice: previous + £1\nAlice2M: Alice at 2 months ago";
			Lang lang = new Lang();
			Business b = lang.parse(s);
			b.setSamples(1);
			b.setColumns(4);
			
			b.run();
			
			double[] bvs = b.getRow("Alice").getValues();
			Assert.assertArrayEquals(bvs, new double[]{1, 2, 3, 4}, 0.1);
			
			double[] delta = b.getRow("Alice2M").getValues();
			Printer.out(b.toCSV());
			Assert.assertArrayEquals(delta, new double[]{0,0,1, 2}, 0.1);
		}
	}

	
	@Test
	public void testDelta2M() {
		{
			String s = "Alice: previous + £1\nAlice2MDelta: (Alice at 2 months ago) - Alice";
			Lang lang = new Lang();
			Business b = lang.parse(s);
			b.setSamples(1);
			b.setColumns(4);
			b.run();
			double[] bvs = b.getRow("Alice").getValues();
			Assert.assertArrayEquals(bvs, new double[]{1, 2, 3, 4}, 0.1);
			
			double[] delta = b.getRow("Alice2MDelta").getValues();
			Printer.out(b.toCSV());
			Assert.assertArrayEquals(delta, new double[]{-1, -2, -2, -2}, 0.1);
		}
	}
	
	@Test
	public void testPrevious() {
		{
			String s = "Bob: previous + £1";
			Lang lang = new Lang();
			Business b = lang.parse(s);
			b.setSamples(1);
			b.setColumns(4);
			b.run();
			double[] bvs = b.getRow("Bob").getValues();
			Assert.assertArrayEquals(bvs, new double[]{1, 2, 3, 4}, 0.1);
		}
		{
			String s = "Bob from month 2: £1";
			Lang lang = new Lang();
			Business b = lang.parse(s);
			b.setSamples(1);
			b.setColumns(4);
			b.run();
			double[] bvs = b.getRow("Bob").getValues();
			Assert.assertArrayEquals(new double[]{0, 1, 1, 1}, bvs, 0.1);
		}
		{
			String s = "Bob from month 2: previous + £1";
			Lang lang = new Lang();
			Business b = lang.parse(s);
			b.setSamples(1);
			b.setColumns(4);
			b.run();
			double[] bvs = b.getRow("Bob").getValues();
			Assert.assertArrayEquals(bvs, new double[]{0, 1, 2, 3}, 0.1);
		}
	}

	@Test
	public void testEvalSeqn() {
		Lang lang = new Lang();
		Business b = lang.parse("Alice: Bob*2\nBob:month");
		b.setColumns(3);
		b.run();
		Row cf = b.getRow("Alice");
		Printer.out(cf.getValues());
		assert cf.getValues()[0] == 2;
		Row balance = b.getRow("Bob");
		Printer.out(balance.getValues());		
	}
	
	@Test(expected=Throwable.class)
	public void testEvalSeqnBad() {
		Lang lang = new Lang();
		{
			Business b = lang.parse("Alice: Bob*2\nBob:Alice");
			b.setColumns(3);
			b.setSamples(1);
			b.run();
		}
		{
			Business b = lang.parse("Alice: Bob*2\nBob:Alice");
			b.setColumns(3);
			b.setSamples(3);
			b.run();
		}
	}

	@Test
	public void testFormulaBug() {
		Lang lang = new Lang();
		Business b = lang.parse("Invest at month 1:£100\nCashflow: -£10 per month\n"
				+"Balance: sum(Invest to date) + sum(Cashflow to date)");
		b.setColumns(3);
		b.run();
		Printer.out("Invest", b.getRow("Invest").getValues());
		Row cf = b.getRow("Cashflow");
		Printer.out("Cashflow", cf.getValues());
		Row balance = b.getRow("Balance");
		Printer.out("Balance", balance.getValues());
		
		Col c2 = new Col(2);
		Rule rule = balance.getRules().get(0);
		b.state = new BusinessState();
		b.put(new Cell(b.getRow("Invest"), new Col(1)), new Numerical(100));
		b.put(new Cell(cf, new Col(1)), new Numerical(-10));
		b.put(new Cell(cf, new Col(2)), new Numerical(-10));
		Numerical v = rule.formula.calculate(new Cell(balance, c2));
		assert v.doubleValue() == 80 : v;
		
		b.run();
		assert cf.getValues()[0] == -10;
		assert cf.getValues()[1] == -10;
		assert balance.getValues()[0] == 90;
		assert balance.getValues()[1] == 80;
	}


	@Test
	public void testFormulaBugUsingPrevious() {
		Lang lang = new Lang();
		Business b = lang.parse("Invest at month 1:£100\nCashflow: -£10 per month\nBalance: previous + Invest + Cashflow");
		b.setColumns(3);
		b.run();
		Printer.out("Invest", b.getRow("Invest").getValues());
		Row cf = b.getRow("Cashflow");
		Printer.out("Cashflow", cf.getValues());
		Row balance = b.getRow("Balance");
		Printer.out("Balance", balance.getValues());
		
		Col c2 = new Col(2);
		Rule rule = balance.getRules().get(0);
		b.state = new BusinessState();
		b.put(new Cell(b.getRow("Invest"), new Col(1)), new Numerical(100));
		b.put(new Cell(cf, new Col(1)), new Numerical(-10));
		b.put(new Cell(cf, new Col(2)), new Numerical(-10));
		Numerical v = rule.formula.calculate(new Cell(balance, c2));
		assert v.doubleValue() == 80 : v;
		
		b.run();
		assert cf.getValues()[0] == -10;
		assert cf.getValues()[1] == -10;
		assert balance.getValues()[0] == 90;
		assert balance.getValues()[1] == 80;
	}

	/**
	 * Hm:
	 * 
	 * In e.g. "Balance: Cashflow + Interest", Cashflow would evauate to the single cell in the same time column.
	 * In e.g. "Balance: sum Cashflow" -- we'd like Cashflow to behave differently -- but is that wrong?
	 * Should it be "Balance: sum Cashflow to now"
	 */
	@Test
	public void testFormulaBugSimple_Known_BAD() {
		Lang lang = new Lang();
		Business b = lang.parse("Cashflow: £10 per month\nBalance: sum Cashflow to date");
		b.setColumns(2);
		b.run();
		Row cf = b.getRow("Cashflow");
		Printer.out("Cashflow", cf.getValues());
		Row balance = b.getRow("Balance");
		Printer.out("Balance", balance.getValues());
		assert Arrays.equals(balance.getValues(), new double[] {10, 20});
//		sum Cashflow needs an implicit to now??
//		row.calculate
	}


	@Test
	public void testRunTestPlan() {
		Parser.DEBUG = false;
		String txt = FileUtils.read(getClass().getResourceAsStream("test-plan.txt"));
		Lang lang = new Lang();
		Business b = lang.parse(txt);
		b.run();
		Printer.out(b.toString());
		Printer.out(b.toCSV());
	}


	@Test
	public void testRunGLPlan() {
		Parser.DEBUG = false;
		String txt = FileUtils.read(new File("plans/gl2020-2021.ms"));
		Lang lang = new Lang();
		Business b = lang.parse(txt);
		b.run();
		Printer.out(b.toString());
		Printer.out(b.toCSV());
	}
	


	@Test
	public void testRunTestPlan2() {
		Parser.DEBUG = false;
		String txt = FileUtils.read(new File("plans/test.ms"));
		Lang lang = new Lang();
		Business b = lang.parse(txt);
		b.run();
		Printer.out(b.toString());
		Printer.out(b.toCSV());
	}


	@Test
	public void testFormulaBugSimplePrev() {
		Lang lang = new Lang();
		Business b = lang.parse("Cashflow: £10 per month\nBalance: Cashflow + previous Cashflow");
		b.setColumns(2);
		b.run();
		Row cf = b.getRow("Cashflow");
		Printer.out(cf.getValues());
		Row balance = b.getRow("Balance");
		Printer.out("Balance", balance.getValues());
		assert Arrays.equals(balance.getValues(), new double[] {10, 20});
	}

	
//	@Test // For now use explicit "to date"
	public void testCellSetImplicitNow() {
		Lang lang = new Lang();
		Business b = lang.parse("Cashflow: £10 per month\nBalance: sum Cashflow");
		BusinessContext.setBusiness(b);		
		b.setColumns(2);				
		
		Row cf = b.getRow("Cashflow");
		Row balance = b.getRow("Balance");
		Rule sumRule = balance.getRules().get(0);
		Col col = b.getColumns().get(0);
		Cell cell = new Cell(balance, col);

		RowName balanceSelector = (RowName) sumRule.getSelector();
		Formula f = sumRule.formula;
//		Collection<Cell> cells = selector.getCells(b);
//		System.out.println(cells);
		b.state = new BusinessState();			
		Numerical b0 = sumRule.calculate(cell);
		assert b0.doubleValue() == 10 : b0;
	}

	@Test
	public void testMonteCarlo() {
		{
			Lang lang = new Lang();
			Business b = lang.parse("columns: 2 months\nAlice: p(0.25) per month");
			b.state = new BusinessState();
			Row alice = b.getRow("Alice");
			Col col1 = new Col(1);
			Numerical v1 = alice.calculate(col1, b);
			Particles1D ps = new Particles1D(new double[0]);
			for(int i=0; i<10000; i++){
				Numerical v = alice.calculate(col1, b);
				if (v==null) v = Numerical.NULL;
				assert v.getClass() == Numerical.class;
				ps.add(v.doubleValue());
			}
			assert MathUtils.approx(ps.getMean(), 0.25) : ps;
		}
		{
			Lang lang = new Lang();
			Business b = lang.parse("columns: 1 month\nAlice: 1 + p(0.25)");
			int n = 10000;
			b.setSamples(n);
			b.run();
			Row alice = b.getRow("Alice");
			UncertainNumerical cell = (UncertainNumerical) b.getCellValue(new Cell(alice, new Col(1)));
			Particles1D dist = (Particles1D) cell.getDist();
			assert dist.pts.length == n;
			assert MathUtils.approx(dist.getMean(), 1.25) : dist;
		}
	}
	
	@Test
	public void testMonteCarloBug() {
		{	// bug with zero handling
			Lang lang = new Lang();
			Business b = lang.parse("columns: 2 months\n\nAlice: p(0.25)");
			int n = 100;
			b.setSamples(n);
			b.run();
			Row alice = b.getRow("Alice");
			UncertainNumerical cell = (UncertainNumerical) b.getCellValue(new Cell(alice, new Col(1)));
			Particles1D dist = (Particles1D) cell.getDist();
			assert dist.pts.length == n;
			assert MathUtils.approx(dist.getMean(), 0.25) : dist;
		}
	}
	
	@Test
	public void testMonteCarloGroupRow() {
		{
			Lang lang = new Lang();
			Business b = lang.parse("columns: 2 months\nStaff:\n"
					+"\tAlice: p(0.5)\n\tBob: Alice");
			// more accuracy than usual for testing
			b.getSettings().setSamples(10000);
			b.run();			
			Row staff = b.getRow("Staff");
			Row alice = b.getRow("Alice");
			Col col1 = new Col(1);
			Cell bc = new Cell(alice, col1);
			UncertainNumerical gv = (UncertainNumerical) staff.getGroupValue(col1, bc);
			IDistribution1D dist = gv.getDist();			
			Printer.out(dist);
			UncertainNumerical a1 = (UncertainNumerical) b.getCellValue(new Cell(alice, col1));
			Printer.out(a1.getDist());
			assert MathUtils.approx(dist.getMean(), 1) : dist;
			// Bob samples = Alice, so variance A+B = var 2*A = 4*var(A) = 1
			assert MathUtils.approx(dist.getVariance(), 1) : dist;
		}
		{
			Lang lang = new Lang();
			Business b = lang.parse("columns: 2 months\nStaff:\n"
					+"\tAlice: p(0.5)\n\tBob: p(0.5)");
			// more accuracy than usual for testing
			b.getSettings().setSamples(1000);
			b.run();			
			Row staff = b.getRow("Staff");
			Col col1 = new Col(1);
			Cell bc = new Cell(staff, col1);
			UncertainNumerical gv = (UncertainNumerical) staff.getGroupValue(col1, bc);
			IDistribution1D dist = gv.getDist();
			Printer.out(dist);
			assert MathUtils.approx(dist.getMean(), 1) : dist;
			// independent, don't get the same multiplier effect, variance is less
			assert dist.getVariance() < 1 : dist;
		}
	}
	
	@Test
	public void testToCSV() {
		Lang lang = new Lang();
		Business b = lang.parse("Alice: £1 per month\nBob:£1 per year\nBoth: sum above");
		b.setColumns(7);
		Row alice = b.getRow("Alice");
		List<Rule> rules = alice.getRules();
		assert rules.size() == 1 : rules;
		Rule rule = rules.get(0);
		Numerical v = rule.calculate(new Cell(alice, new Col(1)));
		assert v.doubleValue() == 1 : v;
		
		Row both = b.getRow("Both");
		b.run();
		BusinessContext.setBusiness(b);
		Col col = new Col(4);		
		Cell bc = new Cell(both, col);
		Rule sum = both.getRules().get(0);
		RowName sumSel = (RowName) sum.getSelector();
		assert sumSel.getRowNames(null).contains("Both");
		
		Formula f = sum.formula;
		Numerical s = sum.calculate(bc);
		assert s.doubleValue() > 1 : s;
		
		Printer.out(rule);
		Printer.out(v);
		String csv = b.toCSV();
		Printer.out(csv);
	}

	@Test
	public void testToCSVGroup() {
		Lang lang = new Lang();
		Business b = lang.parse("Staff:\n\tAlice: £10 per month\n\tBob:£5 per month");
		b.setColumns(3);
		b.run();
		
		String csv = b.toCSV();
		System.out.println(csv);
		assert csv.contains("15") : csv;
	}
	
	@Test
	public void testToCSVNestedGroup() {
		Lang lang = new Lang();
		Business b = lang.parse("Staff:\n\tUK:\n\t\tAlice: £10 per month\n\t\tBob:£5 per month\n\tUS:\n\t\tZak: £2");
		b.setColumns(3);
		b.run();
		
		ArrayMap pij = b.getParseInfoJson();
		System.out.println(pij);
		
		Row uk = b.getRow("UK");
		assert uk.isGroup();
		assert uk.getChildren().size() == 2;
		Col col = new Col(1);
		Numerical sumuk = uk.calculate(col, b);
		assert sumuk.doubleValue() > 0;
		
		String csv = b.toCSV();
		System.out.println(csv);
		assert csv.contains("17") : csv;
		assert csv.contains("15") : csv;
		assert csv.contains("2") : csv;
	}
	
}

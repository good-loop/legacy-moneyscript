package com.winterwell.moneyscript.lang.num;

import static com.winterwell.nlp.simpleparser.Parsers.lit;
import static com.winterwell.nlp.simpleparser.Parsers.seq;

import java.util.Collection;
import java.util.List;

import org.junit.Test;

import com.winterwell.maths.stats.distributions.d1.Gaussian1D;
import com.winterwell.maths.stats.distributions.discrete.ObjectDistribution;
import com.winterwell.moneyscript.lang.Lang;
import com.winterwell.moneyscript.lang.UncertainNumerical;
import com.winterwell.moneyscript.lang.cells.CellSet;
import com.winterwell.moneyscript.lang.time.LangTime;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.BusinessContext;
import com.winterwell.moneyscript.output.BusinessState;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Col;
import com.winterwell.moneyscript.output.Row;
import com.winterwell.nlp.simpleparser.GrammarPrinter;
import com.winterwell.nlp.simpleparser.ParseResult;
import com.winterwell.nlp.simpleparser.Parser;
import com.winterwell.utils.Dep;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;
import com.winterwell.utils.time.Time;

public class LangNumTest {

	

	
	@Test
	public void testAverageFormula() {
		Lang lang = new Lang();
		LangNum nlang = lang.langNum;
		ParseResult<Formula> foo1 = nlang.num.parseOut("average(Staff)");
		ParseResult<Formula> foo = nlang.num.parseOut("average(previous 6 months)");
		System.out.println(foo);		
	}
	
	
	@Test
	public void testHashTagFormula() {
		Lang lang = new Lang();
		LangNum nlang = lang.langNum;
//		ParseResult<Formula> mten = nlang.formula.parseOut("-£10");
		ParseResult<Formula> foo = nlang.cellSetAsFormula.parseOut("Foo#bar");
		Formula foobar = nlang.formula.parseOut("Foo#bar").getX();
		System.out.println(foobar);		
	}
	
	@Test
	public void testGaussian() {
		Lang lang = new Lang();
		// 23k +- 20k
		ParseResult<Formula> n = LangNum.num.parseOut("N(23k, 451m)");		
		BasicFormula formula = (BasicFormula) n.getX();
		UncertainNumerical un = (UncertainNumerical) formula.num;
		Printer.out(formula);
		Printer.out(un.doubleValue());
		for(int i=0; i<10; i++) {
			Printer.out(un.getDist().sample());
		}
	}

	


	@Test
	public void testCount() {
		Lang lang = new Lang();
		ParseResult<Formula> n = LangNum.num.parseOut("count(Staff)");
		Formula f = n.getX();
		
		Business b = lang.parse("Staff:\n\tAlice: £1k\n\tBob from month 3: £2k");
		b.state = new BusinessState(b);
		// just alice
		Numerical c = f.calculate(new Cell(b.getRow("Staff"), new Col(1)));
		assert c.doubleValue() == 1 : c;
		// + Bob
		Numerical c3 = f.calculate(new Cell(b.getRow("Staff"), new Col(3)));
		assert c3.doubleValue() == 2 : c3;
	}
	
	@Test
	public void testCountRow () {
		Lang lang = new Lang();
		{	// specific row
			ParseResult<Formula> n = LangNum.num.parseOut("count row(Bob from month 1 to month 6)");
			Formula f = n.getX();
			
			Business b = lang.parse("Staff:\n\tAlice: £1k\n\tBob from month 3: £2k");
			b.state = new BusinessState(b);
			
			Numerical c = f.calculate(new Cell(b.getRow("Bob"), new Col(6)));
			assert c.doubleValue() == 4 : c; // Note: from - to is INCLUSIVE, so this count picks up 3,4,5,6
		}
		{	// implicit row
			ParseResult<Formula> n = LangNum.num.parseOut("count row(from month 1 to month 6)");
			Formula f = n.getX();
			
			Business b = lang.parse("Staff:\n\tAlice: £1k\n\tBob from month 3: £2k");
			b.state = new BusinessState(b);
			Cell b6 = new Cell(b.getRow("Bob"), new Col(6));
			Numerical c = f.calculate(b6);
			assert c.doubleValue() == 4 : c; // Note: from - to is INCLUSIVE, so this count picks up 3,4,5,6
		}
	}
	

	
	@Test
	public void testPlusMinus() {
		Lang lang = new Lang();
		LangNum.num.parseOut("10 +- 5");
		LangNum.num.parseOut("£54k +- £6k");
		ParseResult<Formula> n = LangNum.num.parseOut("£54k +- £6k per year");		
		Formula formula = n.getX();
		Printer.out(formula);
		Business b = lang.parse("Alice: £54k +- £6k per year");
		Cell cell = new Cell(b.getRows().get(0), new Col(1));
		Numerical v = formula.calculate(cell);
		Printer.out(v);
		assert v.doubleValue() >= 4000 && v.doubleValue() <= 5000 : v;
	}


	
	@Test
	public void testCurrencies() {
		Lang lang = new Lang();
		LangNum.num.parseOut("10");
		LangNum.num.parseOut("£5k");
		// dollars to pounds
		Dep.set(CurrencyConvertor_USD2GBP.class, new CurrencyConvertor_USD2GBP(new Time()));
		Formula tenBucks = LangNum.num.parseOut("$10").getX();
		System.out.println(tenBucks);
		Cell cell = new Cell(null, null);
		Numerical v = tenBucks.calculate(cell);
		assert v.getUnit().equals("£");
		assert v.doubleValue() < 9 : v;
		
		LangNum.num.parseOut("$5k");
	}


//	@Test for now, use explicit "to date" or "to now"
	public void testImplicitToNow_Known_BAD() {
		Lang lang = new Lang();
		Parser<Formula> p = lang.langNum.mathFnUnary;
//		Parser.DEBUG = true;
		ParseResult<Formula> prf = p.parseOut("sum Cashflow");
		Formula f = prf.getX();
		UnaryOp ufo = (UnaryOp) f;
		BasicFormula r = (BasicFormula) ufo.right;
		CellSet cells = r.sel;
		
		Business b = lang.parse("Cashflow: £1\nBalance: sum Cashflow");
		Row row = b.getRow("Cashflow");
		Col col = new Col(2);
		Cell bc = new Cell(row, col);
		Collection<Cell> hm = cells.getCells(bc, true);
		assert hm.size() == 1 : hm.size()+" "+hm;
	}


	@Test
	public void testExplicitToNow() {
		Lang lang = new Lang();
		Parser<Formula> p = lang.langNum.mathFnUnary;
//		Parser.DEBUG = true;
		ParseResult<Formula> prf = p.parseOut("sum(Cashflow to date)");
		Formula f = prf.getX();
		UnaryOp ufo = (UnaryOp) f;
		BasicFormula r = (BasicFormula) ufo.right;
		CellSet cells = r.sel;
		
		Business b = lang.parse("Cashflow: £1\nBalance: sum(Cashflow to date)");
		Cell bc = new Cell(b.getRow("Cashflow"), new Col(2));
		Collection<Cell> hm = cells.getCells(bc, true);
		assert hm.size() == 2 : hm;
	}


	@Test
	public void testPrint() {
		Lang lang = new Lang();
		GrammarPrinter gp = new GrammarPrinter();
		String bnf = gp.print(lang.langNum.mathFnNameUnary);
		System.out.println(bnf);
	}
	
	@Test public void testPrevious() {
		Lang lang = new Lang();
		{	// dummy test
			String plan = "A: 100\n"
						 +"B: 1000 - A\n";			
			Business b = lang.parse(plan);
			b.getSettings().setSamples(1);
			b.setColumns(3);
			b.run();
			Row cap = b.getRow("B");
			double[] bvalues = cap.getValues();
			assert bvalues[0] == 900 : Printer.str(bvalues);
			assert bvalues[1] == 900 : Printer.str(bvalues);
		}
		{
			String plan = "Payment: 100\n"
						 +"Capital: previous - Payment\n"
						 +"Capital at month 1: 1000\n";			
			Business b = lang.parse(plan);
			b.setColumns(3);
			b.run();
			Row cap = b.getRow("Capital");
			assert cap.getValues()[0] == 1000 : Printer.str(cap.getValues());
			assert cap.getValues()[1] == 900 : Printer.str(cap.getValues());
		}
		{	// previous OtherRow
			lang.langNum.mathFnUnary.parseOut("previous(X)");
			lang.langNum.mathFnUnary.parseOut("previous X");
			lang.langNum.num.parseOut("previous(X)");
			lang.langNum.num.parseOut("previous X");
			String plan = "Payment: previous(Capital)*10%\n"
				 		 +"Capital: 1000 - 100*month\n";			
			Business b = lang.parse(plan);
			b.setColumns(3);
			b.run();
			Row cap = b.getRow("Capital");
			Row pay = b.getRow("Payment");
			Printer.out(cap.getValues());
			Printer.out(pay.getValues());
			assert pay.getValues()[0] == 0;
			assert pay.getValues()[1] == 90;
		}
		{	// different order
			// TODO sort rules: rules with a filter beat those without, regardless of definition order
			String plan = "Payment: 100\n"
						 +"Capital at month 1: 1000\n"
						 +"Capital: previous - Payment\n";						 
			Business b = lang.parse(plan);
			b.setColumns(3);
			b.run();
			Row cap = b.getRow("Capital");
			assert cap.getValues()[0] == 1000 : Printer.toString(cap.getValues());
			assert cap.getValues()[1] == 900 : cap.getValues();
		}
	}
	
	@Test public void testFormulaChain() {
		Lang lang = new Lang();
		LangNum ln = lang.langNum;
		Formula f1 = ln.num.parseOut("(2/3)*6").getX();
		assert f1.calculate(null).doubleValue() == 4 : f1; 
		Formula f2 = ln.num.parseOut("(Rate/12)*Capital").getX();		
	}
	
	@Test public void testFormulaBug() {
		Lang lang = new Lang();
		LangNum ln = lang.langNum;
		Formula f2 = ln.num.parseOut("(Rate/12)*Capital").getX();
		{
			Business b = lang.parse("Rate:12%\nCapital:£1k\nInterest: (Rate/12)*Capital");
			// fill in some values
			b.setColumns(2);
			b.run();			
			Row interest = b.getRow("Interest");
			Formula f = interest.getRules().get(0).getFormula();
			Numerical x = f.calculate(new Cell(interest, new Col(1)));
			assert x.doubleValue() == 0.01*1000 : x;
		}
//		Formula f1 = ln.num.parseOut("Rate*Capital/12").getX();;
	}
	
	@Test public void testMortgageFormula() {
		Lang lang = new Lang();
		LangNum ln = lang.langNum;
		{
			ln.mathFnSpecial.parseOut("repay(1,1,1 month)");
			lang.langTime.dt.parseOut("25 years");
			ln.num.parseOut("Capital");
			ln.num.parseOut("Rate");
			ln.num.parseOut("Capital + Rate");
			ln.mathFnSpecial.parseOut("repay(Capital,1,1 month)");
			ln.mathFnSpecial.parseOut("repay((Capital),(Rate),1 month)");			
			ln.mathFnSpecial.parseOut("repay(Capital,(Rate),25 years)");
			
			ParseResult<Formula> pr = ln.mathFnSpecial.parseOut("repay(100k,2.95%,25 years)");
			MortgageFormula f = (MortgageFormula) pr.getX();
			Numerical payment = f.calculate(new Cell(null, new Col(1)));
			assert MathUtils.equalish(payment.doubleValue(), 471.6148) : payment;			
		}
		{
			double Capital = 100000;
			for(int i=0; i<10; i++) {				
				ParseResult<Formula> pr = ln.mathFnSpecial.parseOut("repay("+((int)Capital)+",(12%),10 months)");
				MortgageFormula f = (MortgageFormula) pr.getX();
				double interest = Capital*0.01;
				Numerical payment = f.calculate(new Cell(null, new Col(i+1)));				
				Capital = Capital + interest - payment.doubleValue();
				Printer.out(i+"\t"+Capital+"\t"+payment.doubleValue());
			}		
			assert MathUtils.approx(Capital, 0);
		}		
	}
	
	/**
	 * Test investigating a problem which showed up with the repay(X,Y,Z) command.
	 */
	@Test public void testMortgageCommand() {
		Lang lang = new Lang();
		LangNum ln = lang.langNum;
		ln.num.parseOut("Capital");
		ln.num.parseOut("Rate");
		lang.langTime.dt.parseOut("1 month");
		ParseResult<Formula> pr = ln.num.parse("Rate,1 month");
		assert pr == null;
		Parser repay0 = seq(lit("repay("),
				LangTime.dt, 
				lit(")"));
		repay0.parseOut("repay(1 month)");
		Parser repay1 = seq(lit("repay("), //opt(space),
				ln.num,  
				lit(","), 
				LangTime.dt, 
				lit(")"));
		repay1.parseOut("repay(Rate,1 month)");
		// Aha! a list of names counts as a name?
		// OK - that's fixed now
		assert ln.num.parse("Capital,Rate") == null;
//		Parser.DEBUG = true;		
		Parser repay1b = seq(lit("repay("), 
				ln.num,  
				lit(","), 
				ln.num, 
				lit(")"));
		repay1b.parseOut("repay(Capital,Rate)");
		
		Parser repay = seq(lit("repay("), //opt(space),
				ln.num,  
				lit(","), 
				ln.num,  
				lit(","), 
				LangTime.dt, 
				lit(")"));
		repay.parseOut("repay(Capital,Rate,1 month)");
		ln.mathFnSpecial.parseOut("repay(Capital,Rate,1 month)");
		ln.mathFnSpecial.parseOut("repay(100k,12%,10 months)");
	}
	
	@Test public void testCompoundingFormula() {
		LangNum lang = new LangNum();
		{
			ParseResult<Formula> pr = lang.compoundingFormula.parseOut("+ 1");
			BinaryOp f = (BinaryOp) pr.getX();
			assert f.isStacked() : f;
		}
	}
	
	@Test public void testListValues() {
		Lang lg = new Lang();
		LangNum lang = lg.langNum;
		assert lang.numList.parse("") == null;
		assert lang.numList.parse(",") == null;
		assert lang.numList.parse("1,") == null;		
		{
			ParseResult<List<Formula>> pr = lang.numList.parseOut("1,2");
			List<Formula> list = pr.getX();
			assert list.size() == 2;
			assert list.get(0).calculate(null).doubleValue() == 1;
			assert list.get(1).calculate(null).doubleValue() == 2;
		}
		assert lang.numList.parse("1") == null;
		{
			ParseResult<List<Formula>> pr = lang.numList.parseOut("1, 2, 3");
			List<Formula> list = pr.getX();
			assert list.size() == 3;
			assert list.get(2).calculate(null).doubleValue() == 3;
		}
	}
	
	@Test public void testVar() {
		LangNum lang = new LangNum();
		{
			ParseResult pr = lang.globalVars.parseOut("row");
			Var n = (Var) pr.ast.getX();
		}
		{
			ParseResult pr = lang.num.parseOut("row");
			Var n = (Var) pr.ast.getX();
		}
	}
	
	@Test
	public void testNumber() {
		LangNum lang = new LangNum();
		{
			ParseResult pr = lang._number.parseOut("£10");
			Object n = pr.ast.getX();
			assert n instanceof Numerical;
			assert ((Numerical) n).doubleValue() == 10;
		}
		{
			ParseResult percent = lang._number.parse("10%");
			Numerical p = (Numerical) percent.ast.getX();
			assert p.doubleValue() == 0.1 : p;
		}
		{	// problem caused by , allowed in numbers
			ParseResult pr = lang.gaussian.parseOut("N(10,2)");			
			Gaussian1D dist = (Gaussian1D) ((UncertainNumerical)pr.ast.getX()).getDist();
			assert dist.getMean() == 10;
			assert dist.getVariance() == 2 : dist;
			
			lang.gaussian.parseOut("N(£10,2)");
		}
	}
	

	
	@Test
	public void testNumberCurrency() {
		LangNum lang = new LangNum();
		{
//			Parser.DEBUG = true;
			assert Numerical.number.matcher("1000000.45").find();
			ParseResult pr = lang._number.parseOut("100000.45");
			Object n = pr.ast.getX();
			assert n instanceof Numerical;
			assert ((Numerical) n).doubleValue() == 100000.45;
		}
		
		{
			ParseResult pr = lang._number.parseOut("£100,000");
			Object n = pr.ast.getX();
			assert n instanceof Numerical;
			assert ((Numerical) n).doubleValue() == 100000;
		}
		{
			ParseResult pr = lang._number.parseOut("$100,000");
			Object n = pr.ast.getX();
			assert n instanceof Numerical;
			assert ((Numerical) n).doubleValue() == 100000;
			assert ((Numerical) n).getUnit().equals("$") : n;
		}
		{
			ParseResult pr = lang._number.parseOut("100.45");
			Object n = pr.ast.getX();
			assert n instanceof Numerical;
			assert ((Numerical) n).doubleValue() == 100.45;
		}
		{
			assert Numerical.number.matcher("10.45").find();
			assert Numerical.number.matcher("100.45").find();
			assert Numerical.number.matcher("1000.45").find();
			assert Numerical.number.matcher("10000.45").find();
			assert Numerical.number.matcher("100000.45").find();
			assert Numerical.number.matcher("1000000.45").find();
		}
		{
			ParseResult pr = lang._number.parseOut("100000.45");
			Object n = pr.ast.getX();
			assert n instanceof Numerical;
			assert ((Numerical) n).doubleValue() == 100000.45;
		}
		{
			ParseResult pr = lang._number.parseOut("£100,000.45");
			Object n = pr.ast.getX();
			assert n instanceof Numerical;
			assert ((Numerical) n).doubleValue() == 100000.45;
		}
		{
			ParseResult pr = lang._number.parseOut("$100,000.45");
			Object n = pr.ast.getX();
			assert n instanceof Numerical;
			assert ((Numerical) n).doubleValue() == 100000.45;
			assert ((Numerical) n).getUnit().equals("$") : n;
		}
		{
			ParseResult pr = lang._number.parseOut("-$100,000.45");
			Object n = pr.ast.getX();
			assert n instanceof Numerical;
			assert ((Numerical) n).doubleValue() == -100000.45;
			assert ((Numerical) n).getUnit().equals("$") : n;
		}
	}


	@Test
	public void testProb() {
		LangNum lang = new LangNum();
		lang.mathFnUnary.parseOut("p(0.9)");
		lang.mathFnUnary.parseOut("p(0)");
		lang.mathFnUnary.parseOut("p(0.0)");
		lang.mathFnUnary.parseOut("p(1.0)");
		
		lang.mathFnUnary.parseOut("p(0.5)");
		{
			ParseResult<Formula> pr = lang.mathFnUnary.parseOut("p(10%)");
			double v = pr.ast.getX().calculate(null).doubleValue();
			assert v==0 || v==1;
		}
		{
			Business b = new Business();
			BusinessContext.setBusiness(b);
			Cell bc = new Cell(null, null);
			ParseResult<Formula> pr = lang.mathFnUnary.parseOut("p(20% per year)");
			Formula f = pr.ast.getX();
			double v = pr.ast.getX().calculate(bc).doubleValue();
			assert v==0 || v==1;
			
			ObjectDistribution<Boolean> dist = new ObjectDistribution<Boolean>();
			for(int i=0; i<1000; i++) {
				boolean yes = false;
				for(int m=1; m<13; m++) {
					double x = f.calculate(bc).doubleValue();
					if (x==1) yes = true;
				}
				dist.count(yes);
			}
			Printer.out(dist);
			dist.normalise();
			assert MathUtils.approx(dist.prob(true), 0.2);
		}
	}

	@Test
	public void testTimesPlus() {		
		LangNum lang = new LangNum();
		{
			Parser.DEBUG= true;
			ParseResult<Formula> pr = lang.formula.parseOut("2+5*3");
			Formula f = pr.ast.getX();
			Printer.out(f);
			Numerical x = f.calculate(null);
			assert x.doubleValue() == 17 : x;
		}
		{
			Parser.DEBUG= true;
			ParseResult<Formula> pr = lang.formulaPlus.parseOut("2*5+3");
			Formula f = pr.ast.getX();
			Printer.out(f);
			Numerical x = f.calculate(null);
			assert x.doubleValue() == 13 : x;
		}
		{
			Parser.DEBUG= true;
			ParseResult<Formula> pr = lang.formula.parseOut("2*5+3");
			Formula f = pr.ast.getX();
			Printer.out(f);
			Numerical x = f.calculate(null);
			assert x.doubleValue() == 13 : x;
		}
		{
			Parser.DEBUG= true;
			ParseResult<Formula> pr = lang.formula.parseOut("3*5+7*10");
			Formula f = pr.ast.getX();
			Printer.out(f);
			Numerical x = f.calculate(null);
			assert x.doubleValue() == 85 : x;
		}		
		{
			Parser.DEBUG= true;
			ParseResult<Formula> pr = lang.formula.parseOut("2*2+1+2+3*5+7+11*13*17");
			Formula f = pr.ast.getX();
			Printer.out(f);
			Numerical x = f.calculate(null);
			assert x.doubleValue() == 2460 : x;
		}
	}
	

	@Test
	public void testUniformDIst() {
		LangNum lang = new LangNum();
		ParseResult pr = lang.formula.parseOut("10 +- 2");
		Object x = pr.ast.getX();
		Printer.out(x);
	}
	

	@Test
	public void testFormulaBrackets() {
		LangNum lang = new LangNum();
		lang.formula.parseOut("2 + 2");
		lang.formula.parseOut("(2 + 2)");		
		ParseResult pr = lang.formula.parseOut("3 * (2 + 2)");
		Formula f = (Formula) pr.ast.getX();
		Numerical v = f.calculate(new Cell(new Row("blah"), new Col(1)));
		assert v.doubleValue() == 12 : v;
		lang.formula.parseOut("((2 * 3) + 2)");
	}




	@Test
	public void testArithmetic() {
		LangNum lang = new LangNum();
		Cell cell = new Cell(new Row("blah"), new Col(1));
		{
			Formula f22 = lang.formula.parseOut("2 + 2").getX();
			Numerical v4 = f22.calculate(cell);
			assert v4.doubleValue() == 4;		
		}
		{
			Formula f223 = lang.formula.parseOut("2 + 2 + 3").getX();
			Numerical v7 = f223.calculate(cell);
			assert v7.doubleValue() == 7;
		}
		{
			Formula f223 = lang.formula.parseOut("(2 + 2) + 3").getX();
			Numerical v7 = f223.calculate(cell);
			assert v7.doubleValue() == 7;
		}
		{
			Formula f223 = lang.formula.parseOut("(2 + 2 + 3)").getX();
			Numerical v7 = f223.calculate(cell);
			assert v7.doubleValue() == 7;
		}
		{
			Formula f223 = lang.formula.parseOut("(2 + (2 + 3))").getX();
			Numerical v7 = f223.calculate(cell);
			assert v7.doubleValue() == 7;
		}
		{
			Formula f223 = lang.formula.parseOut("((2 + 2) + 3)").getX();
			Numerical v7 = f223.calculate(cell);
			assert v7.doubleValue() == 7;
		}
		{
			Formula fm102 = lang.formula.parseOut("10 - 2").getX();
			Numerical v = fm102.calculate(cell);
			assert v.doubleValue() == 8;
		}
		{
			Formula fm102 = lang.formula.parseOut("10 - 3 - 1").getX();
			Numerical v = fm102.calculate(cell);
			assert v.doubleValue() == 6;
		}
	}
	
	@Test
	public void testMinusTimes() {
		LangNum lang = new LangNum();
		Cell cell = new Cell(new Row("blah"), new Col(1));
		{
			Formula f = lang.formula.parseOut("2*3 - 5*7").getX();
			Numerical v = f.calculate(cell);
			assert v.doubleValue() == -29 : v;		
		}
		{
			Formula f = lang.formula.parseOut("3 * -2 + 7").getX();
			Numerical v = f.calculate(cell);
			assert v.doubleValue() == 1 : v;		
		}
		{
			Formula f = lang.formula.parseOut("-3 * -2 + 7").getX();
			Numerical v = f.calculate(cell);
			assert v.doubleValue() == 13 : v;		
		}
		{
			Formula f = lang.formula.parseOut("3 - 2 * -2").getX();
			Numerical v = f.calculate(cell);
			assert v.doubleValue() == 7 : v;	
		}
	}
	@Test
	public void testMinusMinus() {		
		LangNum lang = new LangNum();
		{
			ParseResult<Formula> pr = lang.formula.parseOut("1 - 2 - 3");
			Formula f = pr.ast.getX();
			Numerical x = f.calculate(null);
			assert x.doubleValue() == -4 : x;
		}
		{
			ParseResult<Formula> pr = lang.formula.parseOut("10 - 2 + 3");
			Formula f = pr.ast.getX();
			Numerical x = f.calculate(null);
			assert x.doubleValue() == 11;
		}
	}
	
	
	@Test
	public void testSimpleFormula() {		
		LangNum lang = new LangNum();
		{
			ParseResult<Formula> pr = lang.formula.parseOut("1 + 1");
			assert pr.ast.getX() instanceof Formula : pr.ast.getX();
			pr = lang.formula.parseOut("1 * 1");
			assert pr.ast.getX() instanceof Formula;
			pr = lang.formula.parseOut("1 +- 1");	
			assert pr.ast.getX() instanceof Formula;
		}
	}
	
	@Test
	public void testSumOf() {
		LangNum lang = new LangNum();
		{			
			lang.mathFnUnary.parseOut("sum Sales");
		}
		{
			ParseResult pr = lang.mathFnUnary.parseOut("sum Sales to now");
		}
	}
	
	@Test
	public void testNestedFormulaFullyBracketed() {
		LangNum lang = new LangNum();
		{
			lang.formula.parseOut("(1 + (1 + 1))");
			lang.formula.parseOut("((1 + 1) + 1)");
			lang.formula.parseOut("((1 + 1) * 2)");			
		}
		{
			lang.formula.parseOut("(£200 +- 100)");
			lang.formula.parseOut("(100 * Customers)");
			lang.formula.parseOut("((£200 +- 100) * Customers)");
			lang.formula.parseOut("((X+1)^2)");
		}
		{
			Business b = new Lang().parse("Customers:1\nIncome: ((£200 +- 100) * Customers)");
		}		
	}
	
	@Test
	public void testFormula() {
		LangNum lang = new LangNum();
		lang.formula.parseOut("-£10");
		lang.formula.parseOut("100");
		// problem is to do with the loop check in recursion, I think
		// fixed recursion issue, maybe - new problem is back-tracking
		ParseResult pr0 = lang.opMediumBind.parseOut(" * ");
		Printer.out(pr0.ast);
		
		ParseResult pr = lang.formula.parseOut("-£10 * 100");		
		
		lang.formula.parseOut("sum X");
		lang.formula.parseOut("sum X to now");
		lang.formula.parseOut("sum X above");
		
		{
			lang.formula.parseOut("log(5)");
			lang.formula.parseOut("2^3");
			lang.formula.parseOut("(X+1)");
			lang.formula.parseOut("((X+1)^2)");
			lang.formula.parseOut("(X+1)^2");
		}
	}
	
	@Test
	public void testDontStop() {		
		LangNum lang = new LangNum();
		{
			lang.formula.parseOut("(1 + 1) + 2");
			lang.formula.parseOut("(1 + 1) * 2");
			// we parse (1 + 1) then stop!
		}
	}
	
	@Test
	public void testNestedFormula() {		
		LangNum lang = new LangNum();
		{
			lang.formula.parseOut("1 + (1 + 1)");
			lang.formula.parseOut("(1 + (1 + 1))");
			lang.formula.parseOut("((1 + 1) + 1)");
			// brackets making a difference?!
			lang.formula.parseOut("((1 + 1) * 2)");			
			lang.formula.parseOut("(1 + 1) * 2");
			lang.formula.parseOut("(1 + 1) + 2"); // also fails
			// we parse (1 + 1) then stop!
		}
		{
			lang.formula.parseOut("£200 +- 100");
			lang.formula.parseOut("(£200 +- 100)");
			lang.formula.parseOut("100 * Customers");
			lang.formula.parseOut("(£200 +- 100) * Customers");
		}
		{
			Lang lng = new Lang();
			Business b = lng.parse("Customers:1\nIncome: (£200 +- 100) * Customers");
		}
	}
}

package com.winterwell.moneyscript.lang;

import org.junit.Test;

import com.winterwell.moneyscript.lang.num.Numerical;
import com.winterwell.moneyscript.lang.time.LangTime;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.BusinessContext;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Col;
import com.winterwell.moneyscript.output.Row;
import com.winterwell.nlp.simpleparser.Parser;
import com.winterwell.utils.Printer;

public class RuleTest {

	@Test
	public void testFormulaStacking() {
		Parser.DEBUG = false;
		{
			Lang lang = new Lang();
			Business b = lang.parse("Alice: £10 per month\nAlice from month 2: £1 per month");
			b.setColumns(4);
			b.run();
			Row row = b.getRow("Alice");
			Printer.out(row.getValues());
			assert row.getValues()[0] == 10;
//			assert row.getValues()[1] == 11;
//			assert row.getValues()[2] == 11;
		}
		{
			Lang lang = new Lang();
			Business b = lang.parse("Alice: £10 per month\nAlice from month 3: * 150%");
			b.setColumns(4);
			b.run();
			Row row = b.getRow("Alice");
			Printer.out(row.getValues());
			assert row.getValues()[0] == 10;
			assert row.getValues()[2] == 15;
			
			Rule multiplier = row.getRules().get(1);
			Col col = new Col(3);
			Cell bc = new Cell(row, col);
			// row.calculate should be idempotent
			row.calculate(col, b);
			row.calculate(col, b);
			assert row.getValues()[1] == 10 : Printer.toString(row.getValues());
			assert row.getValues()[2] == 15;
			
			Numerical v = multiplier.formula.calculate(bc);
			// repeated apps of formula.calculate will alter things 
			assert v.doubleValue() >= 15 : v;			
		}
	}
	
	@Test
	public void testFromRule() {
		Parser.DEBUG = false;
		Parser.clearGrammar();
		Lang lang = new Lang();
		LangTime lt = new LangTime();
		// weird "time" ref not set bug!
		LangTime.time.parse("dahjdahdasuyyuy");
		{			
			Business b = lang.parse("Alice: £10 per month\nAlice from 1 month from start: £1 per month");
			b.setColumns(4);
			b.run();
			Row row = b.getRow("Alice");
			Printer.out(row.getValues());
		}
		{
			Business b = lang.parse(
					"Staff:\n"
					+"\tAlice: £10 per month\n"
					+"Staff from 1 month from start: £1 per month");
			b.setColumns(4);
			b.run();
			Row row = b.getRow("Alice");
			Printer.out(row.getValues());
		}
		{
			Business b = lang.parse(
					"Staff:\n"
					+"\tAlice from month 2: £10 per month\n"
					+"\tBob: £5 per month\n"
					+"Staff from 1 month from start: £1 per month");
			b.setColumns(4);
			b.run();
			Row row = b.getRow("Alice");
			Printer.out(row.getValues());
			row = b.getRow("Bob");
			Printer.out(row.getValues());
		}
	}
	
	@Test
	public void testPerRow() {
		{
			Business b = new Business();
			BusinessContext.setBusiness(b);
			Row alice = new Row("Alice");
			b.addRow(alice);
			Lang lang = new Lang();
			Rule rule = lang.parseLine("Alice: £10 per month", b);	
			assert rule != null;
			for(int m=1; m<4; m++) {
				Col col = new Col(m);
				Numerical v = rule.calculate(new Cell(alice, col));
				assert v.doubleValue() == 10 : v;
			}
		}
		{
			Business b = new Business();
			BusinessContext.setBusiness(b);
			Row alice = new Row("Alice");
			b.addRow(alice);
			Lang lang = new Lang();
			Rule rule = lang.parseLine("Alice: £120 per year", b);	
			assert rule != null;
			for(int m=1; m<10; m++) {
				Col col = new Col(m);
				Numerical v = rule.calculate(new Cell(alice, col));
				assert v != null : rule;
				assert v.doubleValue() == 10 : v;
			}
		}
	}
	
	@Test
	public void testSetValueRow() {
		Business b = new Business();
		Row alice = new Row("Alice");
		b.addRow(alice);
		Lang lang = new Lang();
		Rule rule = lang.parseLine("Alice at month 1: £10", b);	
		assert rule != null;
		Col col = new Col(1);
		Numerical v = rule.calculate(new Cell(alice, col));
		assert v==null || v.doubleValue() == 10 : v;
		
		Numerical v2 = rule.calculate(new Cell(alice, new Col(2)));
		assert v2==null || v2.doubleValue() == 0 : v2;
		v2 = rule.calculate(new Cell(alice, new Col(3)));
		assert v2==null || v2.doubleValue() == 0 : v2;
	}

}

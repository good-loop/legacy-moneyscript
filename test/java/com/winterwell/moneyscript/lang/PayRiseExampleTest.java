package com.winterwell.moneyscript.lang;

import java.util.List;

import org.junit.Test;

import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Row;
import com.winterwell.utils.Printer;

public class PayRiseExampleTest {


	@Test public void testPayRisePlus() {
		String plan = "Staff:\n\tAlice: £12k per year\n\tBob: £12k per year\n"
					+"Staff from month 3: +10%\nAlice from month 3: + £6k per year\n";
		Lang lang = new Lang();
		Business b = lang.parse(plan);
		b.setColumns(6);
		b.run();
		Row cap = b.getRow("Alice");
		Row pay = b.getRow("Bob");
		Printer.out(cap.getValues());
		Printer.out(pay.getValues());
	}


	@Test public void testPayRiseTimes() {
		String plan = "Staff:\n\tAlice: £12k per year\n\tBob: £12k per year\n"
					+"Staff from month 3: * 110%\nAlice from month 3: + £6k per year\n";
		Lang lang = new Lang();
		Business b = lang.parse(plan);
		b.setColumns(6);
		b.run();
		Row cap = b.getRow("Alice");
		Row pay = b.getRow("Bob");
		String alicePay = Printer.out(cap.getValues());
		String bobPay = Printer.out(pay.getValues());
		assert alicePay.equals("1000, 1000, 1650, 1650, 1650, 1650");
		assert bobPay.equals("1000, 1000, 1100, 1100, 1100, 1100");
	}
	
	@Test public void testPayRiseTimesExcept() {
		String plan = "Staff:\n\tAlice: £12k per year\n\tBob: £12k per year\n"
					+"Staff except(Alice) from month 3: * 110%\nAlice from month 3: + £6k per year\n";
		Lang lang = new Lang();
		Business b = lang.parse(plan);
		b.setColumns(6);
		b.run();
		Row cap = b.getRow("Alice");
		Row pay = b.getRow("Bob");
		String alicePay = Printer.out(cap.getValues());
		String bobPay = Printer.out(pay.getValues());
		Printer.out(pay.getValues());
		List<Row> rows = b.getRows();
		String srows = Printer.str(rows);
		assert srows.equals("Staff, Alice, Bob") : srows;
		assert alicePay.equals("1000, 1000, 1500, 1500, 1500, 1500");
		assert bobPay.equals("1000, 1000, 1100, 1100, 1100, 1100");
	}
}

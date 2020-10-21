package com.winterwell.moneyscript.lang;

import java.util.List;

import org.junit.Test;

import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Col;
import com.winterwell.moneyscript.output.Row;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;
import com.winterwell.utils.StrUtils;


public class MortgageExampleTest {

	
	@Test public void testMortgageOverlySimpleEg() {
		String plan = "columns: 25 years\n"					
					+"Payment: repay(previous Capital, (3.99%), 25 years)\n"
					+"Capital: previous - Payment\n"
					+"Capital at month 1: £100000\n";
		Lang lang = new Lang();
		Business b = lang.parse(plan);
		List<Col> cols = b.getColumns();
		assert cols.size() == 25*12 : cols.size();
		b.setColumns(12);
		b.run();
		Row cap = b.getRow("Capital");
		Row pay = b.getRow("Payment");
		Printer.out(cap.getValues());
		Printer.out(pay.getValues());
	}

	
	@Test public void testMortgageEg() {
		String plan = "columns: 25 years\n"
					+"Rate: 4%\n"
					+"Capital: previous + previous(Interest) - previous(Payment)\n"
					+"Payment: repay(Capital, Rate, 25 years)\n"
					+"Interest: (Capital) * (Rate/12)\n"
					+"Capital at month 1: £100000\n";
		Lang lang = new Lang();
		Business b = lang.parse(plan);
		b.setSamples(1);
		List<Col> cols = b.getColumns();
		assert cols.size() == 25*12 : cols.size();
		b.run();
		Row cap = b.getRow("Capital");
		Row pay = b.getRow("Payment");
		Printer.out(cap.getValues());
		Printer.out(pay.getValues());
		// Hm: the BBC's mortgage calculator gives £533.43 monthly payments for this case
		// - we are £5 off
		// Fencepost error? Or a monthly vs daily error?
	}

	
	@Test public void testMortgages() {
		Lang lang = new Lang();
		mortgages2("Rate: 4%\nRate from year 6: Base+2%", lang);
		mortgages2("Rate: 4.79%", lang);
		mortgages2("Rate: Base + 1.8%", lang);
	}


	private void mortgages2(String rate, Lang lang) {
		String plan = "columns: 5 years\n"
			+"Capital: previous + previous(Interest) - previous(Payment)\n"
			+"Base from month 6: previous + (0.1% +- 0.1%)\n"
			+"Base from month 1 to month 6: 0.5%\n"
			+rate+"\n" //+"Rate: 4%\n"
			+"Payment: repay(Capital, Rate, 25 years)\n"
			+"Interest: (Capital) * (Rate/12)\n"
			+"Capital at month 1: £100000\n";			
			Business b = lang.parse(plan);
//			b.setSamples(1);
			b.run();
			Row cap = b.getRow("Capital");
			Row base = b.getRow("Base");
			Row rates = b.getRow("Rate");
			Row pay = b.getRow("Payment");
			Printer.out(cap.getValues());
			Printer.out(base.getValues());
			Printer.out(rates.getValues());
			Printer.out(pay.getValues());
			double totalPaid = MathUtils.sum(pay.getValues());
			Printer.out("Total Paid: "+StrUtils.toNSigFigs(totalPaid, 3)
					+"	Total Remaining: "+StrUtils.toNSigFigs(cap.getValues()[cap.getValues().length-1], 3));
	}

	
}

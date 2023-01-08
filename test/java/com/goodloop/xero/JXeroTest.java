package com.goodloop.xero;

import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.goodloop.xero.data.Invoice;
import com.winterwell.maths.datastorage.DataTable;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
/**
 * https://api-explorer.xero.com/accounting/invoices/getinvoices
 * @author daniel
 *
 */
public class JXeroTest {

	@Test
	public void testFetchInvoices() {
		JXero jxero = new JXero();
		jxero.init();
		Time s = new Time(2022,10,1);
		List<Invoice> data = jxero.fetchInvoices(s);
		for (Invoice invoice : data) {
			Printer.out(invoice);
		}
	}

	
//	@Test TODO
	public void test2020() {
		JXero jxero = new JXero();
		jxero.init();
		Time s = new Time(2020,1,1);
		Time e = new Time(2020,12,31);
		DataTable<String> data = jxero.fetchProfitAndLoss(s, e, TUnit.YEAR);
		System.out.println(data);
		List<Object> row0 = data.getRowList(0);
		assert row0.size() > 1 : row0;
	}

	@Test
	public void testPayroll() {
		JXero jxero = new JXero();
		jxero.init();
		Time s = new Time(2020,1,1);
		Time e = new Time(2020,12,31);
		DataTable<String> data = jxero.fetchPayroll(s, e);
		System.out.println(data);
	}


	@Test
	public void testDec2020_1Month() {
		JXero jxero = new JXero();
		jxero.init();
		Time s = new Time(2020,12,1);
		Time e = new Time(2020,12,31);
		DataTable<String> data = jxero.fetchProfitAndLoss(s, e, TUnit.MONTH);
		System.out.println(data);
		List<Object> row0 = data.getRowList(0);
		assert row0.size() > 1 : row0;
		
		DataTable<String> dt = data;
		Object[] headers = dt.getRow(0);
		Printer.out(headers);
		Object[] gi = dt.get("Grant income");
		Printer.out(gi);
		assert MathUtils.equalish(
				((Number)gi[1]).doubleValue(),
				54643.94);
	}


	@Test
	public void testOct2021() {
		JXero jxero = new JXero();
		jxero.init();
		Time s = new Time(2021,10,1);
		Time e = new Time(2021,10,31);
		DataTable<String> data = jxero.fetchProfitAndLoss(s, e, TUnit.MONTH);
		System.out.println(data);
		List<Object> row0 = data.getRowList(0);
		assert row0.size() > 1 : row0;
		
		Object[] headers = data.getRow(0);
		Printer.out(headers);
		Object[] gi = data.get("Sales - With Media");
		Printer.out(gi);
		assert ((Number)gi[1]).doubleValue() == 460762.62 : gi[1];
	}

	@Test
	public void testSep2021() {
		JXero jxero = new JXero();
		jxero.init();
		Time s = new Time(2021,9,1);
		Time e = new Time(2021,9,30);
		DataTable<String> data = jxero.fetchProfitAndLoss(s, e, TUnit.MONTH);
		System.out.println(data);
		List<Object> row0 = data.getRowList(0);
		assert row0.size() > 1 : row0;
		
		DataTable<String> dt = data;
		Object[] headers = dt.getRow(0);
		Printer.out(headers);
		Object[] gi = dt.get("Sales - With Media");
		Printer.out(gi);
		assert ((Number)gi[1]).doubleValue() == 412568.59 : gi[1];
	}

	@Test
	public void testSales2021() {
		JXero jxero = new JXero();
		jxero.init();
		Time s = new Time(2021,6,1);
		Time e = new Time(2021,11,30);
		DataTable<String> data = jxero.fetchProfitAndLoss(s, e, TUnit.MONTH);
		System.out.println(data);
		List<Object> row0 = data.getRowList(0);
		assert row0.size() > 1 : row0;
		
		DataTable<String> dt = data;
		Object[] headers = dt.getRow(0);
		Printer.out(headers);
		Object[] gi = dt.get("Sales - With Media");
		Printer.out(gi);
		List<Double> expected = Containers.asList(new double[] {
		484180.27,	// Nov
		460762.62,
		412568.59,
		191259.65,
		247735.02,
		153246.61, // Jun
		});
		Collections.reverse(expected);
		for(int i=1; i<gi.length; i++) {
			Number salesWithMediai = (Number) gi[i];
			assert MathUtils.equalish(salesWithMediai.doubleValue(), expected.get(i-1));
		}
	}
	

	@Test
	public void testSalesFullYear() {
		JXero jxero = new JXero();
		jxero.init();
		Time s = new Time(2020,1,1);
		Time e = new Time(2020,12,31);
		DataTable<String> data = jxero.fetchProfitAndLoss(s, e, TUnit.MONTH);
		System.out.println(data);
		List<Object> row0 = data.getRowList(0);
		assert row0.size() > 1 : row0;
		
		DataTable<String> dt = data;
		Object[] headers = dt.getRow(0);
		Printer.out(headers);
		Object[] gi = dt.get("Sales - With Media");
		Printer.out(gi);
		assert gi.length == 13 : gi.length;
		List<Double> expected = Containers.asList(new double[] {
				30484.96, // Jan 2020
				100812.04,
				6435.02,
				37406.92,
				114795.26,
				77876.20,
				44090.33,						
				21353.55, // Aug
				119730.49,
				133899.27,	
				263932.40,	
				512490.24 // Dec 2020						
		});
		for(int i=1; i<gi.length; i++) {
			Number salesWithMediai = (Number) gi[i];
			Double ei = expected.get(i-1);
			double vi = salesWithMediai.doubleValue();
			assert MathUtils.equalish(vi, ei) : i+" "+vi+" != "+ei;
		}
	}
	
	
	@Test
	public void test18Months() {
		JXero jxero = new JXero();
		jxero.init();
		Time s = new Time(2020,1,1);
		Time e = new Time(2021,6,30);
		DataTable<String> data = jxero.fetchProfitAndLoss(s, e, TUnit.MONTH);
		System.out.println(data);
		List<Object> row0 = data.getRowList(0);
		assert row0.size() > 1 : row0;
		
		DataTable<String> dt = data;
		Object[] headers = dt.getRow(0);
		Printer.out(headers);
		Object[] gi = dt.get("Sales - With Media");
		assert gi.length == 19 : gi.length;
		Printer.out(gi);
		List<Double> expected = Containers.asList(new double[] {
				30484.96, // Jan 2020
				100812.04,
				6435.02,
				37406.92,
				114795.26,
				77876.20,
				44090.33,						
				21353.55, // Aug
				119730.49,
				133899.27,	
				263932.40,	
				512490.24, // Dec 2020
				218722.77, // Jan 2021
				194286.04,
				286953.66,
				306064.82,
				106089.48,				
				153246.61 // June		
				
		});
		for(int i=1; i<gi.length; i++) {
			Number salesWithMediai = (Number) gi[i];
			Double ei = expected.get(i-1);
			double vi = salesWithMediai.doubleValue();
			assert MathUtils.equalish(vi, ei) : i+" "+vi+" != "+ei;
		}
	}
	
	@Test
	public void testDec2020_6months() {
		JXero jxero = new JXero();
		jxero.init();
		Time s = new Time(2020,7,1);
		Time e = new Time(2020,12,31);
		DataTable<String> data = jxero.fetchProfitAndLoss(s, e, TUnit.MONTH);
		System.out.println(data);
		List<Object> row0 = data.getRowList(0);
		assert row0.size() > 1 : row0;
		
		DataTable<String> dt = data;
		int ci = dt.getColumnIndex("2020-12-31");
		Object[] headers = dt.getRow(0);
		Printer.out(headers);
		Object[] gi = dt.get("Grant income");
		// Dec: 54,643.94	Nov: 11,111.11	Oct: 11,111.11 Sep - Aug: 13,680.00 Jul: -
		Number decg = (Number) gi[ci];
		Printer.out(gi);
		assert decg.doubleValue() == 54643.94 : decg;
	}



	@Test
	public void testBalanceSheetDec2020_6months() {
		JXero jxero = new JXero();
		jxero.init();
		Time s = new Time(2020,7,1);
		Time e = new Time(2020,12,31);
		DataTable<String> dt = jxero.fetchBalanceSheet(s, e, TUnit.MONTH);
		List<Object[]> data = dt.getLowLevel();
		System.out.println(data);
		Object[] row0 = data.get(0);
		assert row0.length > 1 : row0;
		
		List col0 = dt.getColumn(0);
		System.out.println(StrUtils.join(col0, "\n"));
		int ci = dt.getColumnIndex("2020-12-31");
		Object[] headers = dt.getRow(0);
		Printer.out(headers);
		Object[] totalBankJulDec = dt.get("Total Bank");
		Printer.out(totalBankJulDec);
//		Total Cash at bank and in hand
//		Dec 836,384.63
//		836,749.99
//		959,291.85
//		1,006,558.88
//		753,700.08
//		Jul 701,838.56
		
		Number decg = (Number) totalBankJulDec[ci];
		Printer.out(totalBankJulDec);
		double[] expected = new double[] {701838.56, 753700.06, 1006558.88, 959291.88, 836750, 836384.62};
		
		for(int i=1; i<totalBankJulDec.length; i++) {
			double banki = MathUtils.toNum(totalBankJulDec[i]);
			assert MathUtils.equalish(banki, expected[i-1]);
		}
	}


	
	@Test
	public void test2020Months() {
		JXero jxero = new JXero();
		jxero.init();
		Time s = new Time(2020,1,1);
		Time e = new Time(2020,12,31);
		DataTable<String> data = jxero.fetchProfitAndLoss(s, e, TUnit.MONTH);
		System.out.println(data);		
		List<Object> row0 = data.getRowList(0);
		assert row0.size() == 13 : row0;
	}
	

	@Test
	public void testJan2021_EndDay() {
		JXero jxero = new JXero();
		jxero.init();
		Time s = new Time(2021,1,1);
		Time e = new Time(2021,1,31);
		DataTable<String> data = jxero.fetchProfitAndLoss(s, e, TUnit.MONTH);
		System.out.println(Printer.toString(data));
		List<Object> row0 = data.getRowList(0);
		assert row0.size() > 1 : row0;
		// Jan 2021 has cost activity on 31st, so this test shows that the end day works to include the full month :)
	}

}

package com.winterwell.moneyscript.lang;

import org.junit.Test;

import com.winterwell.moneyscript.lang.num.Numerical;
import com.winterwell.utils.Printer;

public class NumericalTest {

	@Test
	public void testPercent() {
		{
			Numerical p = new Numerical("50%");
			assert p.doubleValue() == 0.5;
			assert p.toString().equals("50%") : p.toString();
		}
	}

	@Test
	public void testPattern() {
		assert Numerical.number.matcher("12.1").matches();
		assert ! Numerical.number.matcher(" 12.1").matches();
		// comma
		assert Numerical.number.matcher("1,000").matches();
		assert Numerical.number.matcher("20,000,500").matches();
		assert ! Numerical.number.matcher("1,0").matches();		
		assert ! Numerical.number.matcher(",").matches();
		assert ! Numerical.number.matcher("1,").matches();
	}
	

	@Test
	public void testPatternCurrency() {
		assert Numerical.number.matcher("12").matches();
		assert Numerical.number.matcher("£12").matches();
		assert Numerical.number.matcher("$12").matches();
	}
	

	@Test
	public void testCurrency() {
		Numerical tenBucks = new Numerical("$10");
		Numerical tenQuid = new Numerical("£10");
		assert tenQuid.getUnit().equals("£");
		assert tenBucks.getUnit().equals("$");
	}
	
	
	@Test
	public void testToString() {		
		{
			Numerical n = new Numerical("10012.01");
			Printer.out(n.toString());
			assert n.toString().equals("10k");
		}
		{
			Numerical n = new Numerical("10000");
			Printer.out(n);
			assert n.toString().equals("10k");
		}
		{
			Numerical n = new Numerical("0.00018002301");
			Printer.out(n);
			assert n.toString().startsWith("0.00018") : n;
		}
		{
			Numerical n = new Numerical("0.00010772301");
			Printer.out(n);
			assert n.toString().equals("0.000108");
		}
	}
	
	@Test
	public void testNumericalString() {
		{
			Numerical n = new Numerical("10");
			assert n.doubleValue() == 10 : n;
		}			
		{
			Numerical n = new Numerical("1.02");
			assert n.doubleValue() == 1.02 : n;
		}
		{
			Numerical n = new Numerical("-10");
			assert n.doubleValue() == -10 : n;
		}
		{
			Numerical n = new Numerical("£10");
			assert n.doubleValue() == 10 : n;
			assert n.getUnit() == "£" : n;
		}
		{
			Numerical n = new Numerical("1k");
			assert n.doubleValue() == 1000 : n;
		}
		{
			Numerical n = new Numerical("-£15k");
			assert n.doubleValue() == -15000 : n;
			assert n.getUnit() == "£" : n;
		}
		{
			Numerical n = new Numerical("1,000");
			assert n.doubleValue() == 1000 : n;
		}
		{
			Numerical n = new Numerical("10%");
			assert n.doubleValue() == 0.1 : n;
			assert n.getUnit() == "%" : n;
		}
	}

}

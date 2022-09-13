package com.winterwell.moneyscript.lang.num;

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
	public void testPlus() {
		{
			Numerical a = new Numerical("10");
			a.addTag("red");
			Numerical b = new Numerical("5");
			b.addTag("green");
			Numerical c1 = a.plus(b);
			Numerical c2 = b.plus(a);
			assert c1.doubleValue() == 15;
			assert c2.doubleValue() == 15;
			assert c1.value4tag.get("red") == 10;
			assert c2.value4tag.get("red") == 10;
		}
		{
			Numerical a = new Numerical("10");
			a.addTag("red");
			Numerical b = new Numerical("5");
			b.addTag("red");
			Numerical c1 = a.plus(b);
			Numerical c2 = b.plus(a);
			assert c1.doubleValue() == 15;
			assert c2.doubleValue() == 15;
			assert c1.value4tag.get("red") == 15;
			assert c2.value4tag.get("red") == 15;
		}
	}


	@Test
	public void testMinus() {
		{
			Numerical a = new Numerical("10");
			a.addTag("red");
			Numerical b = new Numerical("5");
			b.addTag("green");
			Numerical c1 = a.minus(b);
			Numerical c2 = b.minus(a);
			assert c1.doubleValue() == 5;
			assert c2.doubleValue() == -5;
			assert c1.value4tag.get("red") == 10;
			assert c2.value4tag.get("red") == -10;
		}
		{
			Numerical a = new Numerical("10");
			a.addTag("red");
			Numerical b = new Numerical("5");
			b.addTag("red");
			Numerical c1 = a.minus(b);
			Numerical c2 = b.minus(a);
			assert c1.doubleValue() == 5;
			assert c2.doubleValue() == -5;
			assert c1.value4tag.get("red") == 5;
			assert c2.value4tag.get("red") == -5;
		}
	}

	@Test
	public void testTimesScalar() {
		Numerical a = new Numerical("10");
		a.addTag("red");
		Numerical c1 = a.times(2);
		assert c1.doubleValue() == 20;
		assert c1.value4tag.get("red") == 20;
		Numerical c2 = c1.times(-2);
		assert c2.doubleValue() == - 40;
		assert c2.value4tag.get("red") == -40;
		assert c1.value4tag.get("red") == 20;
	}



	@Test
	public void testTimes() {
		{
			Numerical a = new Numerical("10");
			a.addTag("red");
			Numerical b = new Numerical("5");
			b.addTag("green");
			Numerical c1 = a.times(b);
			Numerical c2 = b.times(a);
			assert c1.doubleValue() == 50;
			assert c2.doubleValue() == 50;
			assert c1.value4tag.get("red") == null : c1.value4tag;
			assert c2.value4tag.get("red") == null;
		}
		{
			Numerical a = new Numerical("10");
			a.addTag("red");
			Numerical b = new Numerical("5");
			b.addTag("red");
			Numerical c1 = a.times(b);
			Numerical c2 = b.times(a);
			assert c1.doubleValue() == 50;
			assert c2.doubleValue() == 50;
			assert c1.value4tag.get("red") == 50 : c1.value4tag;
			assert c2.value4tag.get("red") == 50;
		}
		{
			Numerical a = new Numerical("10");
			a.addTag("red");
			Numerical b = new Numerical("5");
			Numerical c1 = a.times(b);
			Numerical c2 = b.times(a);
			assert c1.doubleValue() == 50;
			assert c2.doubleValue() == 50;
			assert c1.value4tag.get("red") == 50 : c1.value4tag;
			assert c2.value4tag.get("red") == 50;
		}
		{	// mixed tag/residual
			Numerical a = new Numerical("10");
			a.addTag("red");
			Numerical b = new Numerical("5");
			Numerical c = a.plus(b);
			Numerical d = new Numerical("2");
			
			Numerical e1 = c.times(d);
			Numerical e2 = d.times(c);
			
			assert e1.doubleValue() == 30;
			assert e2.doubleValue() == 30;
			assert e1.value4tag.get("red") == 20 : e1.value4tag;
			assert e2.value4tag.get("red") == 20;
		}
		{	// mixed tag/blank
			Numerical a = new Numerical("10");
			a.addTag("red");
			Numerical b = new Numerical("5");
			Numerical c = a.plus(b);
			Numerical d = new Numerical("2");
			
			Numerical e1 = c.times(d);
			Numerical e2 = d.times(c);
			
			assert e1.doubleValue() == 30;
			assert e2.doubleValue() == 30;
			assert e1.value4tag.get("red") == 20 : e1.value4tag;
			assert e2.value4tag.get("red") == 20;
			assert e1.value4tag.get("blue") == null;
		}
		{	// mixed tag
			Numerical a = new Numerical("10");
			a.addTag("red");
			Numerical b = new Numerical("5");
			a.addTag("blue");
			Numerical c = a.plus(b);
			
			Numerical d = new Numerical("2");
			d.addTag("red");
			
			Numerical e1 = c.times(d);
			Numerical e2 = d.times(c);
			
			assert e1.doubleValue() == 30;
			assert e2.doubleValue() == 30;
			assert e1.value4tag.get("red") == 20 : e1.value4tag;
			assert e2.value4tag.get("red") == 20;
			assert e1.value4tag.get("blue") == null;
			assert e2.value4tag.get("blue") == null;
		}
		{	// mixed tag2
			Numerical a = new Numerical("10");
			a.addTag("red");
			Numerical b = new Numerical("5");
			a.addTag("blue");
			Numerical c = a.plus(b);
			
			Numerical d1 = new Numerical("2").addTag("red");
			Numerical d2 = new Numerical("2").addTag("green");
			Numerical d = d1.plus(d2);
			
			Numerical e1 = c.times(d);
			Numerical e2 = d.times(c);
			
			assert e1.doubleValue() == 60;
			assert e2.doubleValue() == 60;
			assert e1.value4tag.get("red") == 20 : e1.value4tag;
			assert e2.value4tag.get("red") == 20;
			assert e1.value4tag.get("blue") == null;
			assert e1.value4tag.get("green") == null;
		}
	}

	
	@Test
	public void testDivide() {
		{
			Numerical a = new Numerical("10");
			a.addTag("red");
			Numerical b = new Numerical("5");
			Numerical c1 = a.divide(b);
			Numerical c2 = b.divide(a);
			assert c1.doubleValue() == 2;
			assert c2.doubleValue() == 0.5;
			assert c1.value4tag.get("red") == 2 : c1.value4tag;
			assert c2.value4tag.get("red") == 0.5 : c2.value4tag;
		}
		{
			Numerical a = new Numerical("10");
			a.addTag("red");
			Numerical b = new Numerical("5");
			b.addTag("green");
			Numerical c1 = a.divide(b);
			Numerical c2 = b.divide(a);
			assert c1.doubleValue() == 2;
			assert c2.doubleValue() == 0.5;
			assert c1.value4tag.get("red") == null : c1.value4tag;
			assert c2.value4tag.get("red") == null;
		}
		{	// matching tags
			Numerical a = new Numerical("10");
			a.addTag("red");
			Numerical b = new Numerical("5");
			b.addTag("red");
			Numerical c1 = a.divide(b);
			Numerical c2 = b.divide(a);
			assert c1.doubleValue() == 2;
			assert c2.doubleValue() == 0.5;
			assert c1.value4tag.get("red") == 2 : c1.value4tag;
			assert c2.value4tag.get("red") == 0.5;
		}
		{	// tag / untagged
			Numerical a = new Numerical("10");
			a.addTag("red");
			Numerical b = new Numerical("5");
			Numerical c1 = a.divide(b);
			Numerical c2 = b.divide(a);
			assert c1.doubleValue() == 2;
			assert c2.doubleValue() == 0.5;
			assert c1.value4tag.get("red") == 2 : c1.value4tag;
			assert c2.value4tag.get("red") == 0.5;
		}
		{	// mixed tag/residual
			Numerical a = new Numerical("10");
			a.addTag("red");
			Numerical b = new Numerical("5");
			Numerical c = a.plus(b);
			Numerical d = new Numerical("2");
			
			Numerical e1 = c.divide(d);
			Numerical e2 = d.divide(c);
			
			assert e1.doubleValue() == 7.5;
			assert e2.doubleValue() == 2.0/15 : e2;
			assert e1.value4tag.get("red") == 5 : e1.value4tag;
			assert e2.value4tag.get("red") == 0.2;
		}
		{	// mixed tag/blank
			Numerical a = new Numerical("10");
			a.addTag("red");
			Numerical b = new Numerical("5");
			Numerical c = a.plus(b); // 15
			Numerical d = new Numerical("2");
			
			Numerical e1 = c.divide(d);
			Numerical e2 = d.divide(c);
			
			assert e1.doubleValue() == 7.5;
			assert e2.doubleValue() == 2.0/15;
			assert e1.value4tag.get("red") == 5 : e1.value4tag;
			assert e2.value4tag.get("red") == 0.2;
			assert e1.value4tag.get("blue") == null;
		}
		{	// mixed tag
			Numerical a = new Numerical("10");
			a.addTag("red");
			Numerical b = new Numerical("5");
			a.addTag("blue");
			Numerical c = a.plus(b);
			
			Numerical d = new Numerical("2");
			d.addTag("red");
			
			Numerical e1 = c.divide(d);
			Numerical e2 = d.divide(c);
			
			assert e1.doubleValue() == 7.5;
			assert e2.doubleValue() == 2.0/15;
			assert e1.value4tag.get("red") == 5 : e1.value4tag;
			assert e2.value4tag.get("red") == 0.2;
			assert e1.value4tag.get("blue") == null;
			assert e2.value4tag.get("blue") == null;
		}
		{	// mixed tag2
			Numerical a = new Numerical("10");
			a.addTag("red");
			Numerical b = new Numerical("5");
			a.addTag("blue");
			Numerical c = a.plus(b);
			
			Numerical d1 = new Numerical("2").addTag("red");
			Numerical d2 = new Numerical("2").addTag("green");
			Numerical d = d1.plus(d2);
			
			Numerical e1 = c.divide(d);
			Numerical e2 = d.divide(c);
			
			assert e1.doubleValue() == 15.0/4;
			assert e2.doubleValue() == 4.0/15;
			assert e1.value4tag.get("red") == 5 : e1.value4tag;
			assert e2.value4tag.get("red") == 0.2;
			assert e1.value4tag.get("blue") == null;
			assert e1.value4tag.get("green") == null;
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

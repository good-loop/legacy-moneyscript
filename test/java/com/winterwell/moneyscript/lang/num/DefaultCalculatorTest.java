package com.winterwell.moneyscript.lang.num;

import static org.junit.Assert.*;

import org.junit.Test;

public class DefaultCalculatorTest {

	@Test
	public void testPlus() {
		{
			Numerical two = new Numerical(2);
			Numerical three = new Numerical(3);
			DefaultCalculator c = new DefaultCalculator();
			Numerical five = c.plus(two, three);
			assert five.doubleValue() == 5;
			assert two.doubleValue() == 2;
			assert three.doubleValue() == 3;
		}
		{
			Numerical two = new Numerical(2);
			two.setTag("#green");
			Numerical three = new Numerical(3);
			three.setTag("#blue");
			DefaultCalculator c = new DefaultCalculator();
			Numerical five = c.plus(two, three);
			assert five.doubleValue() == 5;
			assert two.doubleValue() == 2;
			assert three.doubleValue() == 3;
			assert five.getTagged("#green").doubleValue() == 2;
			assert five.getTagged("#blue").doubleValue() == 3;
			assert five.getTagged("#red").doubleValue() == 0;
			assert two.getTagged("#green").doubleValue() == 2;
			assert two.getTagged("#blue").doubleValue() == 0;
		}
	}

	@Test
	public void testDivide() {
		{
			Numerical six = new Numerical(6);
			Numerical three = new Numerical(3);
			DefaultCalculator c = new DefaultCalculator();
			Numerical two = c.divide(six, three);
			assert six.doubleValue() == 6;
			assert two.doubleValue() == 2;
			assert three.doubleValue() == 3;
		}
		{
			Numerical two = new Numerical(2);
			two.setTag("#green");
			Numerical three = new Numerical(3);
			three.setTag("#blue");
			DefaultCalculator c = new DefaultCalculator();
			Numerical six = c.divide(two, three);
			assert six.doubleValue() == 2.0/3;
			assert two.doubleValue() == 2;
			assert three.doubleValue() == 3;
			assert six.getTagged("#green").doubleValue() == 0;
			assert six.getTagged("#blue").doubleValue() == 0;
			assert six.getTagged("#red").doubleValue() == 0;
			assert two.getTagged("#green").doubleValue() == 2;
			assert two.getTagged("#blue").doubleValue() == 0;
		}
		{
			Numerical two = new Numerical(2);
			two.setTag("#green");
			Numerical three = new Numerical(3);
			DefaultCalculator c = new DefaultCalculator();
			Numerical six = c.divide(two, three);
			assert six.doubleValue() == 2.0/3;
			assert two.doubleValue() == 2;
			assert three.doubleValue() == 3;
			assert six.getTagged("#green").doubleValue() == 2.0/3;
			assert six.getTagged("#red").doubleValue() == 0;
			assert two.getTagged("#green").doubleValue() == 2;
			assert two.getTagged("#blue").doubleValue() == 0;
		}
		{
			Numerical two = new Numerical(2);
			two.setTag("#green");
			Numerical three = two.plus(1);
			DefaultCalculator c = new DefaultCalculator();
			Numerical six = c.divide(two, three);
			assert six.doubleValue() == 2.0/3;
			assert two.doubleValue() == 2;
			assert three.doubleValue() == 3;
			assert six.getTagged("#green").doubleValue() == 2.0/2;
			assert six.getTagged("#red").doubleValue() == 0;
			assert two.getTagged("#green").doubleValue() == 2;
			assert three.getTagged("#green").doubleValue() == 2;
		}
		{
			Numerical two = new Numerical(2);
			two.setTag("#car");
			Numerical three = new Numerical(3);
			three.setTag("#colour.blue");
			DefaultCalculator c = new DefaultCalculator();
			Numerical six = c.divide(two, three);
			assert six.doubleValue() == 2.0/3;
			assert two.doubleValue() == 2;
			assert three.doubleValue() == 3;
			assert six.getTagged("#car").doubleValue() == 2.0/3 : six.getTagged("#car");
			assert six.getTagged("#colour.blue").doubleValue() == 2.0/3;
			assert six.getTagged("#red").doubleValue() == 0;
		}
		{
			Numerical two = new Numerical(2);
			two.setTag("#colour.green");
			Numerical three = new Numerical(3);
			three.setTag("#colour.blue");
			DefaultCalculator c = new DefaultCalculator();
			Numerical six = c.divide(two, three);
			assert six.doubleValue() == 2.0/3;
			assert six.getTagged("#colour.blue").doubleValue() == 0 : six.getTagged("#colour.blue");
		}
		{	// ...swap order
			Numerical two = new Numerical(2);
			two.setTag("#colour.green");
			Numerical three = new Numerical(3);
			three.setTag("#colour.blue");
			DefaultCalculator c = new DefaultCalculator();
			Numerical six = c.divide(three, two);
			assert six.doubleValue() == 3.0/2;
			assert six.getTagged("#colour.blue").doubleValue() == 0 : six.getTagged("#colour.blue");
		}
		{	// mixed colours
			Numerical two = new Numerical(2);
			two.setTag("#colour.green");
			Numerical three = new Numerical(3);
			three.setTag("#colour.blue");
			Numerical five = two.plus(three);
			Numerical four = new Numerical(4).setTag("#product.car");
			
			DefaultCalculator c = new DefaultCalculator();
			Numerical twenty = c.divide(four, five);
			assert twenty.doubleValue() == 4.0/5;
			assert twenty.getTagged("#colour.blue").doubleValue() == 4.0/3 : twenty.getTagged("#colour.blue");
			assert twenty.getTagged("#colour.green").doubleValue() == 4.0/2 : twenty.getTagged("#colour.green");
			assert twenty.getTagged("#product.car").doubleValue() == 4.0/5;
		}
		{	// mixed products
			Numerical two = new Numerical(2);
			two.setTag("#colour.green");
			Numerical three = new Numerical(3);
			three.setTag("#product.truck");
			Numerical five = two.plus(three);
			Numerical four = new Numerical(4).setTag("#product.car");
			
			DefaultCalculator c = new DefaultCalculator();
			Numerical twenty = c.divide(four, five);
			assert twenty.doubleValue() == 4.0/5;
			assert twenty.getTagged("#colour.green").doubleValue() == 4.0/2 : twenty.getTagged("#colour.green");
			assert twenty.getTagged("#product.car").doubleValue() == 4.0/2;
		}
		{	// tag one side only
			Numerical two = new Numerical(2);
			two.setTag("#green");
			Numerical three = new Numerical(3);
			DefaultCalculator c = new DefaultCalculator();
			Numerical six = c.divide(three, two);
			assert six.doubleValue() == 3.0/2;
			assert six.getTagged("#green").doubleValue() == 3.0/2;
			Numerical sixb = c.divide(two, three);
			assert sixb.doubleValue() == 2.0/3;
			assert sixb.getTagged("#green").doubleValue() == 2.0/3;
		}
	}
	
	@Test
	public void testTimes() {
		{
			Numerical two = new Numerical(2);
			Numerical three = new Numerical(3);
			DefaultCalculator c = new DefaultCalculator();
			Numerical six = c.times(two, three);
			assert six.doubleValue() == 6;
			assert two.doubleValue() == 2;
			assert three.doubleValue() == 3;
		}
		{
			Numerical two = new Numerical(2);
			two.setTag("#green");
			Numerical three = new Numerical(3);
			three.setTag("#blue");
			DefaultCalculator c = new DefaultCalculator();
			Numerical six = c.times(two, three);
			assert six.doubleValue() == 6;
			assert two.doubleValue() == 2;
			assert three.doubleValue() == 3;
			assert six.getTagged("#green").doubleValue() == 0;
			assert six.getTagged("#blue").doubleValue() == 0;
			assert six.getTagged("#red").doubleValue() == 0;
			assert two.getTagged("#green").doubleValue() == 2;
			assert two.getTagged("#blue").doubleValue() == 0;
		}
		{
			Numerical two = new Numerical(2);
			two.setTag("#green");
			Numerical three = new Numerical(3);
			DefaultCalculator c = new DefaultCalculator();
			Numerical six = c.times(two, three);
			assert six.doubleValue() == 6;
			assert two.doubleValue() == 2;
			assert three.doubleValue() == 3;
			assert six.getTagged("#green").doubleValue() == 6;
			assert six.getTagged("#red").doubleValue() == 0;
			assert two.getTagged("#green").doubleValue() == 2;
			assert two.getTagged("#blue").doubleValue() == 0;
		}
		{
			Numerical two = new Numerical(2);
			two.setTag("#green");
			Numerical three = two.plus(1);
			DefaultCalculator c = new DefaultCalculator();
			Numerical six = c.times(two, three);
			assert six.doubleValue() == 6;
			assert two.doubleValue() == 2;
			assert three.doubleValue() == 3;
			assert six.getTagged("#green").doubleValue() == 4;
			assert six.getTagged("#red").doubleValue() == 0;
			assert two.getTagged("#green").doubleValue() == 2;
			assert three.getTagged("#green").doubleValue() == 2;
		}
		{
			Numerical two = new Numerical(2);
			two.setTag("#car");
			Numerical three = new Numerical(3);
			three.setTag("#colour.blue");
			DefaultCalculator c = new DefaultCalculator();
			Numerical six = c.times(two, three);
			assert six.doubleValue() == 6;
			assert two.doubleValue() == 2;
			assert three.doubleValue() == 3;
			assert six.getTagged("#car").doubleValue() == 6 : six.getTagged("#car");
			assert six.getTagged("#colour.blue").doubleValue() == 6;
			assert six.getTagged("#red").doubleValue() == 0;
		}
		{
			Numerical two = new Numerical(2);
			two.setTag("#colour.green");
			Numerical three = new Numerical(3);
			three.setTag("#colour.blue");
			DefaultCalculator c = new DefaultCalculator();
			Numerical six = c.times(two, three);
			assert six.doubleValue() == 6;
			assert six.getTagged("#colour.blue").doubleValue() == 0 : six.getTagged("#colour.blue");
		}
		{	// ...swap order
			Numerical two = new Numerical(2);
			two.setTag("#colour.green");
			Numerical three = new Numerical(3);
			three.setTag("#colour.blue");
			DefaultCalculator c = new DefaultCalculator();
			Numerical six = c.times(three, two);
			assert six.doubleValue() == 6;
			assert six.getTagged("#colour.blue").doubleValue() == 0 : six.getTagged("#colour.blue");
		}
		{	// mixed colours
			Numerical two = new Numerical(2);
			two.setTag("#colour.green");
			Numerical three = new Numerical(3);
			three.setTag("#colour.blue");
			Numerical five = two.plus(three);
			Numerical four = new Numerical(4).setTag("#product.car");
			
			DefaultCalculator c = new DefaultCalculator();
			Numerical twenty = c.times(four, five);
			assert twenty.doubleValue() == 20;
			assert twenty.getTagged("#colour.blue").doubleValue() == 12 : twenty.getTagged("#colour.blue");
			assert twenty.getTagged("#colour.green").doubleValue() == 8 : twenty.getTagged("#colour.green");
			assert twenty.getTagged("#product.car").doubleValue() == 20;
		}
		{	// mixed products
			Numerical two = new Numerical(2);
			two.setTag("#colour.green");
			Numerical three = new Numerical(3);
			three.setTag("#product.truck");
			Numerical five = two.plus(three);
			Numerical four = new Numerical(4).setTag("#product.car");
			
			DefaultCalculator c = new DefaultCalculator();
			Numerical twenty = c.times(four, five);
			assert twenty.doubleValue() == 20;
			assert twenty.getTagged("#colour.green").doubleValue() == 8 : twenty.getTagged("#colour.green");
			assert twenty.getTagged("#product.car").doubleValue() == 8;
		}
		{	// tag one side only
			Numerical two = new Numerical(2);
			two.setTag("#green");
			Numerical three = new Numerical(3);
			DefaultCalculator c = new DefaultCalculator();
			Numerical six = c.times(three, two);
			assert six.doubleValue() == 6;
			assert six.getTagged("#green").doubleValue() == 6;
			Numerical sixb = c.times(two, three);
			assert sixb.doubleValue() == 6;
			assert sixb.getTagged("#green").doubleValue() == 6;
		}
	}

//	@Test
	public void testTimesScalar() {
		fail("Not yet implemented");
	}

//	@Test
	public void testUnit() {
		fail("Not yet implemented");
	}

	@Test
	public void testMinus() {
		{
			Numerical two = new Numerical(2);
			Numerical three = new Numerical(3);
			DefaultCalculator c = new DefaultCalculator();
			Numerical five = c.minus(two, three);
			assert five.doubleValue() == -1;
			assert two.doubleValue() == 2;
			assert three.doubleValue() == 3;
		}
		{
			Numerical two = new Numerical(2);
			two.setTag("#green");
			Numerical three = new Numerical(3);
			three.setTag("#blue");
			DefaultCalculator c = new DefaultCalculator();
			Numerical five = c.minus(two, three);
			assert five.doubleValue() == -1;
			assert two.doubleValue() == 2;
			assert three.doubleValue() == 3;
			assert five.getTagged("#green").doubleValue() == 2;
			assert five.getTagged("#blue").doubleValue() == -3;
			assert five.getTagged("#red").doubleValue() == 0;
			assert two.getTagged("#green").doubleValue() == 2;
			assert two.getTagged("#blue").doubleValue() == 0;
		}
	}

}

package com.winterwell.moneyscript.lang;

import static org.junit.Assert.*;

import org.junit.Test;

import com.winterwell.moneyscript.lang.Lang;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Row;
import com.winterwell.utils.Printer;

public class ConditionalFilterTest {

	@Test
	public void testContains() {
		Lang lang = new Lang();
		// note: the per-month is important :( 
		// Otherwise it's a set-value-once rule
		// TODO special marking for set-value, e.g. Invest: £100k once
		Business b = lang.parse("Alice: column\nBob from Alice > 2: 100 per month");
		b.run();
		Row bob = b.getRow("Bob");
		double[] vs = bob.getValues();
		Printer.out(vs);
		assert vs[0] == 0;
		assert vs[1] == 0;
		assert vs[2] == 100; // zero-indexed vs 1-indexed
		assert vs[3] == 100;
		assert vs[4] == 100;
		assert vs[5] == 100;
	}

}

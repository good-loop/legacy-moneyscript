package com.winterwell.moneyscript.lang.num;

import org.junit.Test;

import com.winterwell.moneyscript.lang.Lang;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Row;

public class UnaryOpTest {

	@Test
	public void testCount() {
		Lang lang = new Lang();
		Business b = lang.parse("Staff:\n\tAlice: £20\n\tBob from month 3: £5\nNumStaff: count Staff");
		b.setColumns(5);
		b.run();
		Row ns = b.getRow("NumStaff");
		Numerical a = b.getCell(ns.getIndex(), 1);
		Numerical ab = b.getCell(ns.getIndex(), 4);
		assert a.doubleValue() == 1.0 : a;
		assert ab.doubleValue() == 2.0 : ab;
	}

}

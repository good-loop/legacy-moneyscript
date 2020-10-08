package com.winterwell.moneyscript.lang;

import org.junit.Test;

import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Col;
import com.winterwell.moneyscript.lang.Lang;
import com.winterwell.moneyscript.lang.ListValuesRule;
import com.winterwell.moneyscript.output.Row;
import com.winterwell.moneyscript.lang.num.Numerical;


public class ListValuesRuleTest {

	@Test
	public void testSimple() {
		Lang lang = new Lang();
		{
			Business b = lang.parse("Alice: 1, 2, 3");
			Row alice = b.getRow("Alice");
			ListValuesRule rule = (ListValuesRule) alice.getRules().get(0);
			Col col1 = new Col(1);
			Col col2 = new Col(2);
			Col col4 = new Col(4);
			Numerical v1 = rule.calculate(new Cell(alice, col1));
			Numerical v2 = rule.calculate(new Cell(alice, col2));
			Numerical v3 = rule.calculate(new Cell(alice, col4));
			assert v1.doubleValue() == 1;
			assert v2.doubleValue() == 2;
			assert v3==null || v3.doubleValue() == 0;
		}
	}
	
}

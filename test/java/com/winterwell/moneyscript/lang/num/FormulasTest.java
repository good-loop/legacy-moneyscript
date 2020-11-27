package com.winterwell.moneyscript.lang.num;

import org.junit.Test;

import com.winterwell.moneyscript.lang.Lang;
import com.winterwell.nlp.simpleparser.ParseResult;

public class FormulasTest {

	@Test
	public void testPlusPercentage() {
		Lang lang = new Lang();
		// 23k + 10%
		ParseResult<Formula> n = lang.langNum.formula.parseOut("£1000 + 10%");		
		Formula formula = (Formula) n.getX();		
		Numerical y = formula.calculate(null);
		assert y.doubleValue() == 1100 : y;
	}

	@Test
	public void testPlus() {
		Lang lang = new Lang();
		// 23k + 10%
		ParseResult<Formula> n = lang.langNum.formula.parseOut("£1000 + 10");		
		Formula formula = (Formula) n.getX();				
		Numerical y = formula.calculate(null);
		assert y.doubleValue() == 1010;
	}

}

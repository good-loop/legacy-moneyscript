package com.winterwell.moneyscript.lang.bool;

import org.junit.Test;

import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Col;
import com.winterwell.moneyscript.lang.Lang;
import com.winterwell.moneyscript.output.Row;
import com.winterwell.moneyscript.lang.bool.Condition;
import com.winterwell.moneyscript.lang.bool.LangBool;
import com.winterwell.moneyscript.lang.cells.ConditionalFilter;
import com.winterwell.moneyscript.lang.cells.Filter;
import com.winterwell.moneyscript.lang.cells.LangCellSet;
import com.winterwell.moneyscript.lang.cells.LangFilter;
import com.winterwell.moneyscript.lang.cells.SimpleLangCellSet;
import com.winterwell.moneyscript.lang.num.LangNum;
import com.winterwell.moneyscript.lang.num.SimpleLangNum;
import com.winterwell.nlp.simpleparser.ParseResult;

/**
 * @tested {@link LangConditionals}
 * @author daniel
 *
 */
public class LangConditionalsTest {


	@Test
	public void testConditionSimpleNum() {
		SimpleLangNum num = new SimpleLangNum();
		LangBool lang = new LangBool();
		{
			ParseResult pr = LangBool.bool.parseOut("1 > 2");
			Condition c = (Condition) pr.ast.getX();
			Row row = new Row("Dummy");
			Col col = new Col(1);
			boolean f = c.contains(new Cell(row, col), null);
			assert ! f : f;
		}
	}
	
	@Test
	public void testCondition() {
		Lang lang = new Lang();
		LangBool lb = lang.langBool;
		LangCellSet lcs = new LangCellSet();
		LangNum ln = new LangNum();
		{			
			lcs.rowName.parseOut("Sales");
			LangNum.num.parseOut("Sales");
			LangBool.bool.parseOut("Sales > 2");
		}
	}
	
	@Test
	public void testFilter() {
		SimpleLangNum simpleNum = new SimpleLangNum();
		LangBool lang = new LangBool();
		LangCellSet lcs = new SimpleLangCellSet();
		LangFilter lf = new LangFilter();
		{
//			ParseResult<Condition> tst = lang.bool.parseOut("1 > 2");
			LangFilter.filter.parseOut("if 1>2");
			ParseResult pr = LangFilter.filter.parseOut("from 1 > 2");
			Filter f = (Filter) pr.ast.getX();
			assert f != null;
			assert f.getClass() != Filter.class;
		}
		{
			LangNum langNum = new LangNum();
			lang.bool.parseOut("Sales > 2");
			LangFilter.filter.parseOut("if Sales > 2");
			ParseResult pr = LangFilter.filter.parseOut("from Sales > 2");
			ConditionalFilter f = (ConditionalFilter) pr.ast.getX();
			assert f != null;			
		}
	}
}

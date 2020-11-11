package com.winterwell.moneyscript.lang.cells;

import static org.junit.Assert.*;

import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;

import com.winterwell.moneyscript.lang.Lang;
import com.winterwell.moneyscript.lang.Rule;
import com.winterwell.moneyscript.lang.num.BasicFormula;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Col;
import com.winterwell.moneyscript.output.Row;
import com.winterwell.utils.Printer;

public class FilteredCellSetTest {

	@Test
	public void testFilterAgo() {
		{
			String s = "Alice: previous + £1\nAlice2M: Alice at 2 months ago";
			Lang lang = new Lang();
			Business b = lang.parse(s);
			b.setSamples(1);
			b.setColumns(4);
			
			Row row = b.getRow("Alice2M");
			Rule rule = row.getRules().get(0);
			BasicFormula bf = (BasicFormula) rule.formula;
			FilteredCellSet sel = (FilteredCellSet) bf.getCellSetSelector();
			System.out.println(sel);
			Cell focalCell = new Cell(row, new Col(3));
			System.out.println(focalCell);
			Collection<Cell> cells = sel.getCells(focalCell, false);			
			System.out.println(cells);
			// TODO check outputs with asserts
		}		
	}

}

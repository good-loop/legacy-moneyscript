package com.winterwell.moneyscript.lang.time;

import static org.junit.Assert.*;

import org.junit.Test;

import com.winterwell.moneyscript.lang.Lang;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Col;
import com.winterwell.utils.time.Time;

public class TimeDescTest {

	@Test
	public void testGetCol() {
		Lang lang = new Lang();
		Business b = lang.parse("start: Jan 2022\nend: Mar 2022\nAlice: 1");
		SpecificTimeDesc sdt = new SpecificTimeDesc(new Time(2021,6,1), "Jun 2021");
		Cell cell = new Cell(b.getRow("Alice"), new Col(1));
		Col c = sdt.getCol(cell);
		assert c.index < cell.col.index : c;
	}

}

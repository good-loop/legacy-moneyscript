package com.winterwell.moneyscript.lang;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.winterwell.moneyscript.lang.num.Numerical;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Row;
import com.winterwell.utils.time.Time;

public class CompareCommandTestTest {


	@Test
	public void testTimeRow() {
		CompareCommand ic = new CompareCommand("https://docs.google.com/spreadsheets/d/19P7GrukxDJhpuQS0BzF62jJC9nSxR8GU4EvB0lCvXPU/gviz/tq?tqx=out:csv&sheet=P%26L");
		ic.overwrite = true;
		ic.timeRow = 1;
		Lang lang = new Lang();
		Business bs = lang.parse("Grant income: £2");
		bs.getSettings().setStart(new Time(new Time().getYear(), 1, 1));
		bs.getSettings().setEnd(new Time(new Time().getYear(), 12, 31));
		bs.addImportCommand(ic);
		bs.run();
		Row brow = bs.getRow("Grant income");
		List<Map> vs = brow.getValuesJSON(true);
		for (Map map : vs) {
			Object d = map.get("delta");
			if (d!=null) System.out.println(map);
		}
	}

	
		@Test
		public void testBusinessRun_compare() {
			CompareCommand ic = new CompareCommand(new File("test/test-input.csv").toURI().toString());
			ic.overwrite = true;
			Lang lang = new Lang();
			Business bs = lang.parse("Bob: £2");
			bs.getSettings().setStart(new Time(new Time().getYear(), 1, 1));
			bs.getSettings().setEnd(new Time(new Time().getYear(), 12, 31));
			bs.addImportCommand(ic);
			bs.run();
			Row brow = bs.getRow("Bob");
			Numerical c1 = bs.getCell(brow.getIndex(), 0);
			Numerical c5 = bs.getCell(brow.getIndex(), 5);
			List<Map> vs = brow.getValuesJSON(true);
//			System.out.println(Printer.toString(vs, "\n"));
			assert c5.getDelta() == null;
			assert c1.getDelta() == -2;
//			System.out.println(bs.toCSV());
//			System.out.println(bs.toJSON());
		}

}

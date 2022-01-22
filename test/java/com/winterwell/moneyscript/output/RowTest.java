package com.winterwell.moneyscript.output;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.winterwell.moneyscript.lang.Lang;
import com.winterwell.utils.Printer;

public class RowTest {

	@Test
	public void testGetValuesJSON() {
		{	// year totals - saving pocketmoney
			String s = "start: June 2020\nend: Dec 2021\nAlice: previous + £1";
			Lang lang = new Lang();
			Business b = lang.parse(s);
			b.setSamples(1);
			b.run();
			Row row = b.getRow("Alice");
			double[] bvs = row.getValues();
			String sbvs = Printer.out(bvs);
			Assert.assertEquals(sbvs,"1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19");
			List<Map> json = row.getValuesJSON(true);
			Printer.out(json);
			// should be name + values + 1 annual total
			assert json.size() == bvs.length + 2 : json.size()+" vs "+bvs.length;
		}
	}

	

	@Test
	public void testGetValuesJSONShorter() {
		{	// year totals - saving pocketmoney
			String s = "start: Jan 2020\nend: March 2020\nAlice: previous + £1";
			Lang lang = new Lang();
			Business b = lang.parse(s);
			assert b.getSettings().getStart().toISOStringDateOnly().equals("2020-01-01") : b.getSettings().getStart();
			assert b.getSettings().getEnd().toISOStringDateOnly().equals("2020-03-31") : b.getSettings().getEnd();
			b.setSamples(1);
			b.run();
			Row row = b.getRow("Alice");
			double[] bvs = row.getValues();
			String sbvs = Printer.out(bvs);
			Assert.assertEquals(sbvs,"1, 2, 3");
			List<Map> json = row.getValuesJSON(true);
			Printer.out(json);
		}
	}

}

package com.winterwell.moneyscript.lang;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.goodloop.gsheets.GSheetsClientTest;
import com.winterwell.moneyscript.data.PlanDoc;
import com.winterwell.moneyscript.data.PlanSheet;
import com.winterwell.moneyscript.lang.time.LangTime;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.nlp.simpleparser.ParseResult;
import com.winterwell.utils.containers.ArrayMap;

public class ExportCommandTest {

	@Test
	public void testExportAnnualOnly() throws Exception {
		String script = "start: June 2020\nend: Dec 2021\n"
				+ "Alice: 1";
		Lang lang = new Lang();
		Business biz = lang.parse(script);
		biz.run();

		ExportCommand ex0 = new ExportCommand("https://docs.google.com/spreadsheets/d/1vksonmI0OWqshxPb7rNd5cKAMUbXZDwc1E0U6unSGw0");
		ex0.colFreq = KColFreq.ONLY_ANNUAL;
		ex0.runExport(new PlanDoc(), biz);
	}



	@Test
	public void testExport2Sheets() throws Exception {
		PlanSheet s1 = new PlanSheet("start: June 2020\nend: Dec 2021\n"
				+ "Alice: 1");
		PlanSheet s2 = new PlanSheet("start: June 2020\nend: Dec 2021\n"
				+ "Bob: 1 + previous");
		Lang lang = new Lang();
		Business biz = lang.parse(Arrays.asList(s1,s2), null);
		biz.run();

		
		
		ExportCommand ex0 = new ExportCommand("https://docs.google.com/spreadsheets/d/1Kx3Efvz--XkCf4vuPBVZvP2WUctITEH_uyTJ0K6Akkg");
		ex0.gsheetForPlanSheetId = new ArrayMap(
			s1.getId(), "by-order",
			s2.getId(), "by-order"
		);
		
		PlanDoc pd = new PlanDoc();
		ex0.runExport(pd, biz);
	}

}

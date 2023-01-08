package com.winterwell.moneyscript.lang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.winterwell.moneyscript.data.PlanDoc;
import com.winterwell.moneyscript.data.PlanSheet;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.time.Time;

public class ExportCommandTest {


	@Test
	public void testExportFormulae2Sheets() throws Exception {
		Lang lang = new Lang();
		List<PlanSheet> scripts = new ArrayList();
		PlanSheet pd1 = new PlanSheet("Staff:\n"
				+ "    Alice: 1\n"
				+ "    Bob: Alice + 2.5");
		pd1.setTitle("Sheet 1");
		scripts.add(pd1);
		PlanSheet pd2 = new PlanSheet("Sales: 2 * Staff");
		pd2.setTitle("Sheet 2");
		scripts.add(pd2);
		Settings settings = new Settings();
		settings.setStart(new Time(2023,1,1));
		settings.setEnd(new Time(2023,2,28));
		Business biz = lang.parse(scripts, settings);
		biz.run();

		String url = "https://docs.google.com/spreadsheets/d/1cfwJZT6RjjrPH9V-e1vyRKEvtnLKgQu2GzYsHUeG0SU";
		ExportCommand ex0 = new ExportCommand(url);
		ex0.colFreq = KColFreq.ONLY_MONTHLY;
		ex0.preferFormulae = true;
		PlanDoc pd = new PlanDoc();
		pd.setSheets(scripts);
		ex0.runExport(pd, biz);
	}


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

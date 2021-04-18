package com.winterwell.moneyscript.lang;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import com.goodloop.gsheets.GSheetsClientTest;
import com.winterwell.moneyscript.data.PlanDoc;
import com.winterwell.moneyscript.output.Business;

public class ExportCommandTest {

	@Test
	public void testExportAnnualOnly() throws Exception {
		String script = "start: June 2020\nend: Dec 2021\n"
				+ "export annual: https://docs.google.com/spreadsheets/d/1vksonmI0OWqshxPb7rNd5cKAMUbXZDwc1E0U6unSGw0\n"
				+ "Alice: 1";
		Lang lang = new Lang();
		Business biz = lang.parse(script);
		biz.run();
		
		List<ExportCommand> ex = biz.getExportCommands();
		assert ex.size() == 1 : ex;
		ExportCommand ex0 = ex.get(0);
		ex0.runExport(new PlanDoc(), biz);
	}

}

package com.winterwell.moneyscript.webapp;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;

import org.junit.Test;

import com.goodloop.gsheets.GSheetsClient;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.winterwell.moneyscript.data.PlanDoc;
import com.winterwell.moneyscript.lang.Lang;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.nlp.simpleparser.Parser;
import com.winterwell.utils.Printer;
import com.winterwell.utils.io.FileUtils;

public class GSheetFromMSTest {

	@Test
	public void testExcel() throws Exception {
		GSheetFromMS gs4ms = new GSheetFromMS(new GSheetsClient());
		PlanDoc pd = new PlanDoc();
		pd.setText("Staff:\n\tAlice: £1 per month\n\tBob: Alice + £1");
		Business biz = MoneyServlet.lang.parse(pd.getText());
		biz.setColumns(3);
		List<List<Object>> vs = gs4ms.updateValues(biz);
		System.out.println(vs);
	}
	
	@Test
	public void testExcelGPlan() throws Exception {
		Parser.DEBUG = false;
		String txt = FileUtils.read(new File("plans/gl2020-2021.ms"));
		Lang lang = new Lang();
		Business b = lang.parse(txt);

		GSheetFromMS gs4ms = new GSheetFromMS(new GSheetsClient());
		List<List<Object>> vs = gs4ms.updateValues(b);
		System.out.println(vs);
	}
	
	@Test
	public void testPublishExcelGPlan() throws Exception {
		Parser.DEBUG = false;
		String txt = FileUtils.read(new File("plans/gl2020-2021.ms"));
		PlanDoc pd = new PlanDoc();

		GSheetsClient sc = new GSheetsClient();
		Spreadsheet s = sc.createSheet("Test testPublishExcelGPlan");
		pd.setGsheetId(s.getSpreadsheetId());

		pd.setText(txt);			
		GSheetFromMS gs4ms = new GSheetFromMS(new GSheetsClient());
		gs4ms.doExportToGoogle(pd);
		
	}

}

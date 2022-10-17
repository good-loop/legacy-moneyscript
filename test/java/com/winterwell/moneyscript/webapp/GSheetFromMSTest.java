package com.winterwell.moneyscript.webapp;

import java.io.File;
import java.util.List;

import org.junit.Test;

import com.goodloop.gsheets.GSheetsClient;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.winterwell.moneyscript.data.PlanDoc;
import com.winterwell.moneyscript.lang.ExportCommand;
import com.winterwell.moneyscript.lang.Lang;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.nlp.simpleparser.Parser;
import com.winterwell.utils.Printer;
import com.winterwell.utils.io.FileUtils;

public class GSheetFromMSTest {

	@Test
	public void testExcel() throws Exception {
		PlanDoc pd = new PlanDoc();
		pd.setText("Staff:\n\tAlice: £1 per month\n\tBob: Alice + £1");
		Business biz = MoneyServlet.lang.parse(pd.getText());
		biz.setColumns(3);
		GSheetFromMS gs4ms = new GSheetFromMS(new GSheetsClient(), new ExportCommand(""), biz, null);
		gs4ms.setupRows();
		List<List<Object>> vs = gs4ms.calcValues(biz);
		System.out.println(vs);
	}
	

	@Test
	public void testIncludeAnnuals() throws Exception {
		PlanDoc pd = new PlanDoc();
		pd.setText("Alice: £10 per month\nBob: £5\nAlice at month 1: £5");
		Business biz = MoneyServlet.lang.parse(pd.getText());
		biz.setColumns(14);
		
		ExportCommand ec = new ExportCommand("");
		GSheetFromMS gs4ms = new GSheetFromMS(new GSheetsClient(),ec,biz, null);
		gs4ms.incYearTotals = true;
		
		gs4ms.setupRows();
		
		List<List<Object>> vs = gs4ms.calcValues(biz);
		List<Object> headers = vs.get(0);
		assert Printer.toString(headers).contains("Total 20") : headers;
		List<Object> aliceRow = vs.get(1);
//		Printer.out(headers);
//		Printer.out(aliceRow);
		assert aliceRow.size() == headers.size() : aliceRow.size()+" vs "+headers.size();
		assert headers.size() > 15;
	}
	
	
	@Test
	public void testBrackets() throws Exception {
		PlanDoc pd = new PlanDoc();
		pd.setText("Alice: £10 per month\nBob: £5/2 + (2% + 8%) * Alice");
		Business biz = MoneyServlet.lang.parse(pd.getText());
		biz.setColumns(3);
		
		GSheetFromMS gs4ms = new GSheetFromMS(new GSheetsClient(),new ExportCommand(""),biz, null);
		gs4ms.setupRows();
		List<List<Object>> vs = gs4ms.calcValues(biz);
		System.out.println(vs);
	}
	
	@Test
	public void testExcelGPlan() throws Exception {
		Parser.DEBUG = false;
		String txt = FileUtils.read(new File("plans/gl2020-2021.ms"));
		Lang lang = new Lang();
		Business b = lang.parse(txt);

		GSheetFromMS gs4ms = new GSheetFromMS(new GSheetsClient(),new ExportCommand(""),b, null);
		gs4ms.setupRows();
		
		List<List<Object>> vs = gs4ms.calcValues(b);
		System.out.println(vs);
	}
	
	@Test
	public void testPublishExcelGPlan() throws Exception {
		Parser.DEBUG = false;
		String txt = FileUtils.read(new File("plans/gl2020-2021.ms"));
		PlanDoc pd = new PlanDoc();

		GSheetsClient sc = new GSheetsClient();
		Spreadsheet s = sc.createSheet("Test testPublishExcelGPlan");
		ExportCommand ec = new ExportCommand(s.getSpreadsheetId());

		pd.setText(txt);			
		GSheetFromMS gs4ms = new GSheetFromMS(new GSheetsClient(),ec,pd.getBusiness(), null);
		gs4ms.doExportToGoogle();
		
	}

}

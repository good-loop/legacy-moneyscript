package com.goodloop.gsheets;

import java.awt.Color;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jetty.util.ajax.JSON;
import org.junit.Test;

import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.winterwell.maths.GridInfo;
import com.winterwell.utils.Printer;
import com.winterwell.utils.containers.IntRange;

/**
 * @tested {@link GSheetsClient}
 * @author daniel
 *
 */
public class GSheetsClientTest {

	@Test
	public void testGetSheet() throws Exception {        
        final String spreadsheetId = "1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms";        
        GSheetsClient sq = new GSheetsClient();
        Spreadsheet s = sq.getSheet(spreadsheetId);
        System.out.println(s);
        assert s != null;

	}
	

//	@Test // fails for now - but helpfully
	public void testAuthOnline() throws Exception {        
        final String spreadsheetId = "1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms";
        GSheetsClient sq = new GSheetsClient();
        sq.setAccessType("online");
        sq.setUserId("daniel@good-loop.com");
        sq.setRedirectUri("https://localmoneyscript.good-loop.com/oauthcallback");
        Spreadsheet s = sq.getSheet(spreadsheetId);
        System.out.println(s);
        assert s != null;

	}

	@Test
	public void testAuthOffline() throws Exception {        
        final String spreadsheetId = "1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms";
        GSheetsClient sq = new GSheetsClient();
        sq.setAccessType("offline");
        Spreadsheet s = sq.getSheet(spreadsheetId);
        System.out.println(s);
        assert s != null;

	}


	/**
	 * 
 Odd bug Sep 2022: This code would work from the moneyscript project -- but the same code 
 (using the same logins/moneyscript app credentials file)
 would throw a 403 error when run from the calstat project.
	 * @throws Exception
	 */
	@Test
	public void testClearSheet() throws Exception {        
        final String spreadsheetId = "1aQMJI6Plui9TdHeWWCGaoJfnoZB1IbOVu6LDyKrogBI";        
        GSheetsClient sq = new GSheetsClient();
        Spreadsheet s = sq.getSheet(spreadsheetId);
        System.out.println(s);
        
        sq.clearSpreadsheet(spreadsheetId);
        
        Spreadsheet s2 = sq.getSheet(spreadsheetId);
        System.out.println(s2);
	}
	

	@Test
	public void testGetData() throws Exception {        
        final String spreadsheetId = "1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms";        
        GSheetsClient sq = new GSheetsClient();
        List<List<Object>> data = sq.getData(spreadsheetId, "A:A", null);
        assert ! data.isEmpty();

	}
	
	/**
	 * Just a test sheet on Google
	 * https://docs.google.com/spreadsheets/d/1vksonmI0OWqshxPb7rNd5cKAMUbXZDwc1E0U6unSGw0/edit#gid=0
	 */
	public static String sid = "1vksonmI0OWqshxPb7rNd5cKAMUbXZDwc1E0U6unSGw0";
	
//	@Test // fills the drive with junk 'cos I can't find the delete option
	public void testCreateSheet() throws Exception {
        GSheetsClient sq = new GSheetsClient();        
        Spreadsheet s2 = sq.createSheet("Test testCreateSheet");
        String sid = s2.getSpreadsheetId();
        System.out.println(sid);
        assert sid != null;
        
//        sq.todoUpdateSheet(sid);
	}
	

//	@Test // fills the drive with junk 'cos I can't find the delete option
	public void testCreateSheetTitle() throws Exception {
        GSheetsClient sq = new GSheetsClient();
        Spreadsheet s2 = sq.createSheet("Foo Test testCreateSheetTitle");
        String sid = s2.getSpreadsheetId();
        System.out.println(sid);
        assert sid != null;
	}

	@Test
	public void testUpdateValues() throws GeneralSecurityException, IOException {
		GSheetsClient sq = new GSheetsClient();
		List<List<Object>> vs = Arrays.asList(
			Arrays.asList("row","jan","feb","mar"),
			Arrays.asList("Alice", "apples in January", "avocados in Feb", 0),
			Arrays.asList("Bob", "berries in Jan", "blueberries in Feb", 100)
		);
		sq.updateValues(sid, vs);
	}


	@Test
	public void testUpdateValues2Sheets() throws GeneralSecurityException, IOException {
		List<List<Object>> vs1 = Arrays.asList(
			Arrays.asList("row","jan","feb","mar"),
			Arrays.asList("Alice", "apples in January", "avocados in Feb", 0),
			Arrays.asList("Bob", "berries in Jan", "blueberries in Feb", 100)
		);
		List<List<Object>> vs2 = Arrays.asList(
				Arrays.asList("row","jan","feb","mar"),
				Arrays.asList("America", 1,3,5),
				Arrays.asList("Britain", 2,4,6)
			);
		String sid2 = "1Kx3Efvz--XkCf4vuPBVZvP2WUctITEH_uyTJ0K6Akkg";
		GSheetsClient sq = new GSheetsClient();
		List<SheetProperties> sheets = sq.getSheetProperties(sid2);
		Printer.out(sheets);
		
		sq.setSheet(sheets.get(0).getSheetId());
		sq.updateValues(sid2, vs1);
		
		sq.setSheet(sheets.get(1).getSheetId());
		sq.updateValues(sid2, vs2);
	}


	@Test
	public void testUpdateStyle() throws GeneralSecurityException, IOException {
		GSheetsClient sq = new GSheetsClient();
		Request req = sq.setStyleRequest(new IntRange(2, 3),null, Color.green, (Color)null);
		sq.doBatchUpdate(sid, Arrays.asList(req));
	}
	

	@Test
	public void testGetBase26() throws GeneralSecurityException, IOException {
		GSheetsClient sq = new GSheetsClient();
		String a = sq.getBase26(0);
		String z = sq.getBase26(25);
		String aa = sq.getBase26(26);
		String ab = sq.getBase26(27);
		String d = sq.getBase26(123);
		assert a.equals("A") : a;
		assert ab.equals("AB");
		assert z.equals("Z");
	}

}

package com.goodloop.gsheets;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.winterwell.web.app.Logins;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @testedby GSheetsClientTest
 * 
 * @author Google, daniel
 *
 */
public class GSheetsClient {
	private static final String APP = "moneyscript.good-loop.com";
	private static final String APPLICATION_NAME = "MoneyScript by Good-Loop";
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
	private static final String TOKENS_DIRECTORY_PATH = "tokens";

	public Spreadsheet getSheet(String id) throws Exception {
		Sheets service = getService();
		Spreadsheet ss = service.spreadsheets().get(id).execute();
		return ss;
	}

	public Spreadsheet createSheet() throws Exception {
		Sheets service = getService();
		Spreadsheet ss = new Spreadsheet();
//	    	ss.setSpreadsheetId("canwereallysetit"); // No!
//	    	List<Sheet> sheets = ss.getSheets(); // null
//	    	sheets = new ArrayList();
//	    	Sheet sheet = new Sheet();
//	    	sheets.add(sheet);	    	
//	    	ss.setSheets(sheets);
		Spreadsheet s2 = service.spreadsheets().create(ss).execute();
		String sid = s2.getSpreadsheetId();
		return s2;
	}

	/**
	 * Global instance of the scopes required by this quickstart. If modifying these
	 * scopes, delete your previously saved tokens/ folder.
	 */
	private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);
//	    private static final String CREDENTIALS_FILE_PATH = "config/credentials.json";

	/**
	 * Creates an authorized Credential object.
	 * 
	 * @param HTTP_TRANSPORT The network HTTP Transport.
	 * @return An authorized Credential object.
	 * @throws IOException If the credentials.json file cannot be found.
	 */
	private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
		// Load client secrets.
		File credsFile = Logins.getFile(APP, "credentials.json");
		if (credsFile == null) {
			throw new FileNotFoundException(Logins.getLoginsDir()+"/"+APP+"/credentials.json");
		}
		InputStream in = new FileInputStream(credsFile);
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

		// Build flow and trigger user authorization request.
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY,
				clientSecrets, SCOPES)
						.setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
						.setAccessType("offline").build();
		LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
		return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
	}

	/**
	 * Prints the names and majors of students in a sample spreadsheet:
	 * https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit
	 * 
	 * @throws Exception
	 */
	public static void main(String... args) throws Exception {
		// Build a new authorized API client service.
		Sheets service = getService();
		final String spreadsheetId = "1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms";

		GSheetsClient sq = new GSheetsClient();
		Spreadsheet s = sq.getSheet(spreadsheetId);

		Spreadsheet s2 = sq.createSheet();
		String sid = s2.getSpreadsheetId();

		final String range = "Class Data!A2:E";
		ValueRange response = service.spreadsheets().values().get(spreadsheetId, range).execute();

//	        Spreadsheet ss = new Spreadsheet();	        
//	        https://developers.google.com/sheets/api/guides/values
//			Create ssr = service.spreadsheets().create(ss);

		List<List<Object>> values = response.getValues();
		if (values == null || values.isEmpty()) {
			System.out.println("No data found.");
		} else {
			System.out.println("Name, Major");
			for (List row : values) {
				// Print columns A and E, which correspond to indices 0 and 4.
				System.out.printf("%s, %s\n", row.get(0), row.get(4));
			}
		}
	}

	private static Sheets getService() throws GeneralSecurityException, IOException {
		final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
		Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
				.setApplicationName(APPLICATION_NAME).build();
		return service;
	}

	/**
	 * See https://developers.google.com/sheets/api/guides/batchupdate#java
	 * 
	 * @throws IOException
	 * @throws GeneralSecurityException
	 */
	public Object updateValues(String spreadsheetId, List<List<Object>> values)
			throws GeneralSecurityException, IOException {
		Sheets service = getService();
		ValueRange body = new ValueRange().setValues(values);

		String valueInputOption = "USER_ENTERED";
		int w = values.get(0).size();
		String c = getBase26(w - 1); // w base 26
		String range = "A1:" + c + values.size();
		UpdateValuesResponse result = service.spreadsheets().values().update(spreadsheetId, range, body)
				.setValueInputOption(valueInputOption).execute();
		String ps = result.toPrettyString();
		Integer cnt = result.getUpdatedCells();
//			System.out.println(ps);
		return result.toPrettyString();
	}

	/**
	 * 
	 * @param w
	 * @return 0 = A
	 */
	public String getBase26(int w) {
		assert w >= 0 : w;
		int a = 'A';
		if (w < 26) {
			char c = (char) (a + w);
			return Character.toString(a + w);
		}
		int low = w % 26;
		int high = (w / 26) - 1; // -1 'cos this isnt true base 26 -- there's no proper 0 letter
		return getBase26(high) + getBase26(low);
	}

}
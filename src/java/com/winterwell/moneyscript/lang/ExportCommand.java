package com.winterwell.moneyscript.lang;

import com.goodloop.gsheets.GSheetsClient;

public class ExportCommand {

	public String spreadsheetId;

	public ExportCommand(String gSheetUrlOrId) {
		spreadsheetId = GSheetsClient.getSpreadsheetId(gSheetUrlOrId);
		if (spreadsheetId==null) spreadsheetId = gSheetUrlOrId;
	}

}

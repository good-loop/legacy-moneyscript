package com.winterwell.moneyscript.lang;

import com.goodloop.gsheets.GSheetsClient;
import com.winterwell.moneyscript.lang.cells.CellSet;
import com.winterwell.moneyscript.lang.num.Formula;

public class ExportCommand {

	public String spreadsheetId;

	public ExportCommand(String gSheetUrlOrId) {
		spreadsheetId = GSheetsClient.getSpreadsheetId(gSheetUrlOrId);
		if (spreadsheetId==null) spreadsheetId = gSheetUrlOrId;
	}

}

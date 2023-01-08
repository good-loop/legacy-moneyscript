package com.winterwell.moneyscript.data;

import com.winterwell.utils.Utils;

public class PlanSheet {

	String id = Utils.getNonce();
	
	public String getId() {
		if (id==null) id = Utils.getNonce();
		return id;
	}
	String title;
	String text;
	private Integer gsheetId;
	public String gsheetTitle;
	
	public PlanSheet() {
	}
	public PlanSheet(String text) {
		this.text = text;
	}

	public String getText() {
		return text;
	}
	@Override
	public String toString() {
		return "PlanSheet[id=" + id + ", title=" + title + "]";
	}
	public String getTitle() {
		return title;
	}
	public void setGSheetMatch(Integer sheetId, String title2) {
		this.gsheetId = sheetId;
		this.gsheetTitle = title2;
	}
	public void setTitle(String string) {
		this.title = string;
	}
	
}

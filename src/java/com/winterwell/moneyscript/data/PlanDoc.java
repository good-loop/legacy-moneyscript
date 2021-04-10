package com.winterwell.moneyscript.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.winterwell.data.AThing;
import com.winterwell.es.ESKeyword;
import com.winterwell.es.ESNoIndex;
import com.winterwell.moneyscript.lang.ImportCommand;
import com.winterwell.utils.containers.Containers;

public class PlanDoc extends AThing {

	/**
	 * This copies the info in Business for save purposes.
	 */
	List<Map> importCommands = new ArrayList<>();
	
	@ESKeyword
	String gsheetId;
	
	public void setGsheetId(String gsheetId) {
		this.gsheetId = gsheetId;
	}
	
	public String getGsheetId() {
		return gsheetId;
	}
	
	String text;	
	
	public transient Map parseInfo;
	
	@ESNoIndex 
	public List errors;
	
	public String getText() {
		return text;
	}

	public void setText(String s) {
		this.text = s;
	}
	
	public void setImportCommands(List<ImportCommand> importCommands2) {
		if (importCommands2==null) {
			importCommands = null;
			return;
		}
		importCommands = Containers.apply(importCommands2, ImportCommand::toJson2);
	}
}

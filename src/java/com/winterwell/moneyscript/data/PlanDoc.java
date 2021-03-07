package com.winterwell.moneyscript.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.winterwell.data.AThing;
import com.winterwell.es.ESKeyword;
import com.winterwell.es.ESNoIndex;
import com.winterwell.moneyscript.lang.ImportCommand;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.containers.ArrayMap;

public class PlanDoc extends AThing {

	/**
	 * This copies the info in Business for save purposes.
	 */
	List<ImportCommand> importCommands = new ArrayList<>();
	
	@ESKeyword
	String gsheetId;
	
	public void setGsheetId(String gsheetId) {
		this.gsheetId = gsheetId;
	}
	
	public String getGsheetId() {
		return gsheetId;
	}
	
	@Deprecated
	String text;
	
	/**
	 * name to text
	 */
	Map<String,String> texts = new ArrayMap();
	
	public transient Map parseInfo;
	
	@ESNoIndex 
	public List errors;
	
	public String getText() {
		if (texts!=null && ! texts.isEmpty()) {
			String _text = StrUtils.join(texts.values(), "\n\n");
			return _text;
		}
		return text;
	}

	@Deprecated
	public void setText(String s) {
		setText("main", s);
	}
	
	public void setText(String sheetName, String script) {
		if (texts==null) texts = new ArrayMap();
		texts.put(sheetName, script);
	}

	public void setImportCommands(List<ImportCommand> importCommands2) {
		this.importCommands = importCommands2;
	}
}

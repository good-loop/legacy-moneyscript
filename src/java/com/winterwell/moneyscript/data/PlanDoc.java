package com.winterwell.moneyscript.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.winterwell.data.AThing;
import com.winterwell.es.ESKeyword;
import com.winterwell.es.ESNoIndex;
import com.winterwell.moneyscript.lang.ExportCommand;
import com.winterwell.moneyscript.lang.ImportCommand;
import com.winterwell.moneyscript.lang.Lang;
import com.winterwell.moneyscript.lang.Settings;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.log.Log;

public class PlanDoc extends AThing {

	static Lang lang = new Lang();
	
	public transient Business business;

	List charts;
	
	@ESNoIndex 
	public List errors;
	
	Settings settings;
	
	/**
	 * 
	 */
	List<ExportCommand> exportCommands = new ArrayList<>();	
	
	/**
	 * @deprecated This copies the info in Business for save and API / UX purposes.
	 */
	List<Map> importCommands = new ArrayList<>();
	
	/**
	 * If this was a copy - keep the link for info
	 */
	@ESKeyword
	String originalId;

	public transient Map parseInfo;
	
	List<PlanSheet> sheets;

	@Deprecated // replaced by sheets
	private String text;
	
	public Business getBusiness() {
		if (business==null) {
			business = lang.parse(getSheets(), null);
			if (business.getSettings() != settings) {				
				settings = business.getSettings(); 
				Log.d("PlanDoc", "Update settings from parse: "+settings);
			}
		}
		return business;
	}

	public List<ExportCommand> getExportCommands() {
		return exportCommands;
	}
	
	public List<PlanSheet> getSheets() {
		if (sheets==null) {
			sheets = new ArrayList();
			sheets.add(new PlanSheet(text));
		}
		return sheets;
	}
	
	/**
	 * @deprecated normally set by incoming json from the client
	 * @param sheets
	 */
	public void setSheets(List<PlanSheet> sheets) {
		this.sheets = sheets;
	}
	
	@Deprecated // replaced by sheets
	public String getText() {
		// if sheets are in use, then combine them.
		if (sheets!=null && ! sheets.isEmpty()) {
			text = StrUtils.join(Containers.apply(sheets, PlanSheet::getText), "\n\n");
		}
		return text;
	}

	public void setExportCommands(List<ExportCommand> importCommands2) {
		if (importCommands2==null) {
			exportCommands = null;
			return;
		}
		exportCommands = importCommands2;
	}

	public void setImportCommands(List<ImportCommand> importCommands2) {
		if (importCommands2==null) {
			importCommands = null;
			return;
		}
		importCommands = Containers.apply(importCommands2, ImportCommand::toJson2);
	}
	
	public void setText(String s) {
		this.text = s;
		business = null;
	}

	public Settings getSettings() {
		return settings;
	}

}

package com.winterwell.moneyscript.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.winterwell.data.AThing;
import com.winterwell.es.ESKeyword;
import com.winterwell.es.ESNoIndex;
import com.winterwell.moneyscript.lang.ExportCommand;
import com.winterwell.moneyscript.lang.ImportCommand;
import com.winterwell.moneyscript.lang.Lang;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.containers.Containers;

public class PlanDoc extends AThing {

	/**
	 * @deprecated This copies the info in Business for save and API / UX purposes.
	 */
	List<Map> importCommands = new ArrayList<>();
	
	/**
	 * @deprecated copies the info in Business for API / UX purposes.
	 */
	List<ExportCommand> exportCommands = new ArrayList<>();

	/**
	 * If this was a copy - keep the link for info
	 */
	@ESKeyword
	String originalId;
	
	@Deprecated // replaced by sheets
	private String text;	
	
	public transient Map parseInfo;
	
	@ESNoIndex 
	public List errors;

	public transient Business business;
	
	@Deprecated // replaced by sheets
	public String getText() {
		// if sheets are in use, then combine them.
		if (sheets!=null && ! sheets.isEmpty()) {
			text = StrUtils.join(Containers.apply(sheets, PlanSheet::getText), "\n\n");
		}
		return text;
	}

	public List<PlanSheet> getSheets() {
		if (sheets==null) {
			sheets = new ArrayList();
			sheets.add(new PlanSheet(text));
		}
		return sheets;
	}
	
	List<PlanSheet> sheets;

	public void setText(String s) {
		this.text = s;
		business = null;
	}
	
	static Lang lang = new Lang();
	
	public void setImportCommands(List<ImportCommand> importCommands2) {
		if (importCommands2==null) {
			importCommands = null;
			return;
		}
		importCommands = Containers.apply(importCommands2, ImportCommand::toJson2);
	}

	public void setExportCommands(List<ExportCommand> importCommands2) {
		if (importCommands2==null) {
			exportCommands = null;
			return;
		}
		exportCommands = importCommands2;
	}

	public List<ExportCommand> getExportCommands() {
		return exportCommands;
	}
	
	public Business getBusiness() {
		if (business==null) {
			business = lang.parse(getSheets());	
		}
		return business;
	}

}

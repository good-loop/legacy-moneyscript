package com.winterwell.moneyscript.data;

import com.winterwell.utils.Utils;
import com.winterwell.utils.log.Log;

public class PlanSheet {

	String id = Utils.getNonce();
	
	public String getId() {
		if (id==null) id = Utils.getNonce();
		return id;
	}
	String title;
	String text;
	
	public PlanSheet() {
	}
	public PlanSheet(String text) {
		this.text = text;
	}

	public String getText() {
		return text;
	}
	
}

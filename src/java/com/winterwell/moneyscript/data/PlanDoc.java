package com.winterwell.moneyscript.data;

import java.util.List;
import java.util.Map;

import com.winterwell.data.AThing;
import com.winterwell.es.ESNoIndex;

public class PlanDoc extends AThing {

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
}

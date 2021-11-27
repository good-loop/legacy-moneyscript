package com.winterwell.moneyscript.webapp;

import java.util.List;

import com.goodloop.gsheets.GSheetsClient;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.winterwell.utils.Dep;
import com.winterwell.web.ajax.JSend;
import com.winterwell.web.app.IServlet;
import com.winterwell.web.app.WebRequest;

public class GSheetServlet implements IServlet {
	
	@Override
	public void process(WebRequest state) throws Exception {
		if (state.actionIs("info")) {
			String sid = state.getSlugBits(1);
			GSheetsClient gsc = Dep.get(GSheetsClient.class);
			
			List<SheetProperties> sprops = gsc.getSheetProperties(sid);
			
			JSend jsend = new JSend(sprops);
			jsend.send(state);
			return;
		}
	}

}

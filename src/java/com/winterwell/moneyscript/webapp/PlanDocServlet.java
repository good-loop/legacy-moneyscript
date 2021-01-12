package com.winterwell.moneyscript.webapp;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.goodloop.gsheets.GSheetsClient;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.winterwell.data.KStatus;
import com.winterwell.es.ESPath;
import com.winterwell.es.client.KRefresh;
import com.winterwell.moneyscript.data.PlanDoc;
import com.winterwell.moneyscript.lang.GroupRule;
import com.winterwell.moneyscript.lang.Lang;
import com.winterwell.moneyscript.lang.Rule;
import com.winterwell.moneyscript.lang.UncertainNumerical;
import com.winterwell.moneyscript.lang.num.Numerical;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Col;
import com.winterwell.moneyscript.output.Row;
import com.winterwell.nlp.simpleparser.ParseExceptions;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.IHasJson;
import com.winterwell.web.WebEx.E403;
import com.winterwell.web.ajax.AjaxMsg;
import com.winterwell.web.ajax.JThing;
import com.winterwell.web.app.AppUtils;
import com.winterwell.web.app.CrudServlet;
import com.winterwell.web.app.WebRequest;

public class PlanDocServlet extends CrudServlet<PlanDoc> {

	public PlanDocServlet() {
		super(PlanDoc.class);
		augmentFlag = true;
	}

	@Override
	protected void doBeforeSaveOrPublish(JThing<PlanDoc> _jthing, WebRequest stateIgnored) {
		super.doBeforeSaveOrPublish(_jthing, stateIgnored);
		_jthing.java().errors = new ArrayList();
	}

	
	@Override
	protected JThing<PlanDoc> doPublish(WebRequest state, KRefresh forceRefresh, boolean deleteDraft) throws Exception {
		// normal
		JThing<PlanDoc> pubd = super.doPublish(state, forceRefresh, deleteDraft);
		// plus export to google ?? do this async?
		try {
			doExportToGoogle(pubd.java(), state, forceRefresh, deleteDraft);
		} catch(Throwable ex) {
			state.addMessage(AjaxMsg.error(ex));
		}
		return pubd;
	}
	
	private void doExportToGoogle(PlanDoc pd, WebRequest state, KRefresh forceRefresh, boolean deleteDraft) throws Exception {		
		GSheetsClient sc = new GSheetsClient();
		// get/create
		if (pd.getGsheetId() == null) {
			Log.i("Make G-Sheet...");
			Spreadsheet s = sc.createSheet();
			pd.setGsheetId(s.getSpreadsheetId());
			// publish again
			doPublish(state, forceRefresh, deleteDraft);
		}
		
		// parse
		Business biz = MoneyServlet.lang.parse(pd.getText());		
		// run
		biz.run();
		
		List<List<Object>> values = new ArrayList();
		
		List<Col> cols = biz.getColumns();		
		List<Object> headers = new ArrayList();
		headers.add("Row");
		for (Col col : cols) {
			headers.add(col.getTimeDesc());
		}
		values.add(headers);
				
		// a blank row
		final List<Object> blanks = new ArrayList();
		for(int i=0; i<headers.size(); i++) blanks.add("");
						
		List<Row> rows = biz.getRows();

		// HACK - space with a blank row?
		List<Row> spacedRows = new ArrayList();
		int prevLineNum = 0;
		for (Row row : rows) {
			// HACK - space with a blank row?
			Rule r0 = row.getRules().get(0);
			int lineNum = r0.getLineNum();
			if (lineNum > prevLineNum+1) {				
				spacedRows.add(null);
			}
			prevLineNum = lineNum;
			spacedRows.add(row);
		}
		// HACK: put some blanks at the end (to handle a few rows being removed at a time)
		for(int i=0; i<10; i++) spacedRows.add(null);		
		
		// convert		
		for (Row row : spacedRows) {
			if (row==null) {
				values.add(blanks);
				continue;
			}
			Rule r0 = row.getRules().get(0);			
			List<Object> rowvs = new ArrayList();
			rowvs.add(row.getName());
			Collection<Cell> cells = row.getCells();
			for (Cell cell : cells) {
				// group rule?
				if (row.getRules().size()==1) {
					if (r0 instanceof GroupRule) {
						GroupRule gr = (GroupRule) r0;
						List<Row> kids = row.getChildren();
						if ( ! Utils.isEmpty(kids)) {
							StringBuilder sb = new StringBuilder("=");							
							for (Row kid : kids) {
								int ki = spacedRows.indexOf(kid)+2; // +1 for 0 index and +1 for the header row
								sb.append(sc.getBase26(cell.col.index)+ki);
								sb.append(" + ");
							}
							StrUtils.pop(sb, 3);
//							System.out.println(sb);
							rowvs.add(sb.toString());
							continue;
						}
					}					
				} // ./convert rule
				
				Numerical v = biz.getCellValue(cell);
				if (v ==null) {
					rowvs.add(""); 
				} else if (v instanceof UncertainNumerical) {
					rowvs.add(v.doubleValue());	
				} else {
					rowvs.add(v.doubleValue()); // toExportString());
				}				
			} // ./cell
			values.add(rowvs);
		}
		
		// update with data		
		sc.updateValues(pd.getGsheetId(), values);
	}

	@Override
	protected void augment(JThing<PlanDoc> jThing, WebRequest state) {
		// parse and add parse-info
		PlanDoc plandoc = jThing.java();
		try {
			Lang lang = MoneyServlet.lang;			
			String text = plandoc.getText();
			Business biz = lang.parse(text);			
			Map pi = biz.getParseInfoJson();
			if ( ! Utils.isEmpty(plandoc.errors)) {
				plandoc.errors = null;
				jThing.setJava(plandoc);
			}
		} catch(ParseExceptions exs) {
			plandoc.errors = Containers.apply(exs.getErrors(), IHasJson::toJson2);
			Log.d("parse.error", exs);
			jThing.setJava(plandoc);
		} catch(Throwable ex) {
			Log.e("parse", ex);
			// NB same json format as ParseFail
			plandoc.errors = Arrays.asList(new ArrayMap(
				"@type", ex.getClass().getSimpleName(),
				"message", com.winterwell.utils.Printer.toString(ex, true)
			));
			jThing.setJava(plandoc);
		}
	}
	
	
	@Override
	protected JThing<PlanDoc> getThingFromDB(WebRequest state) throws E403 {
		// HACK file
		String sbit1 = state.getSlugBits(1);
		if (sbit1!=null && sbit1.startsWith("file-")) {
			String sf = FileUtils.safeFilename(sbit1.substring(5), true);
			File f = new File("plans", sf);
			String s = FileUtils.read(f);
			PlanDoc pd = new PlanDoc();
			pd.id = state.getSlug();
			pd.setText(s);
			return new JThing().setType(PlanDoc.class).setJava(pd);
		}
		// normal - db
		return super.getThingFromDB(state);
	}
}

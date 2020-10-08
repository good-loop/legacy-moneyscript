package com.winterwell.moneyscript.webapp;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

import com.sun.org.apache.xml.internal.serialize.Printer;
import com.winterwell.es.IESRouter;
import com.winterwell.moneyscript.data.PlanDoc;
import com.winterwell.moneyscript.lang.Lang;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.nlp.simpleparser.ParseExceptions;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.web.WebEx.E403;
import com.winterwell.web.ajax.JSend;
import com.winterwell.web.ajax.JThing;
import com.winterwell.web.app.CrudServlet;
import com.winterwell.web.app.WebRequest;

public class PlanDocServlet extends CrudServlet<PlanDoc> {

	public PlanDocServlet() {
		super(PlanDoc.class);
		augmentFlag = true;
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
//			plandoc.parseInfo = pi; This upsets gson :(
			plandoc.errors = null;
		} catch(ParseExceptions exs) {
			plandoc.errors = Containers.apply(exs.getErrors(), e -> e.toString());
		} catch(Throwable ex) {
			Log.e("parse", ex);
			plandoc.errors = Arrays.asList(com.winterwell.utils.Printer.toString(ex, true));
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

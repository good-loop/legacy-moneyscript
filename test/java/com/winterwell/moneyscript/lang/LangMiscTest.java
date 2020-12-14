package com.winterwell.moneyscript.lang;

import java.util.Map;

import org.junit.Test;

import com.winterwell.moneyscript.output.Business;
import com.winterwell.nlp.simpleparser.ParseResult;
import com.winterwell.utils.Printer;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;

public class LangMiscTest {

	@Test
	public void testJsonLike() {
		Lang lang = new Lang();
		LangMisc lm = lang.langMisc;
		
		Map jobj0 = lm.jsonLike.parseOut("{\"hello\":\"world\"}").getX();
		Map jobj0b = lm.jsonLike.parseOut("{hello:world}").getX();
		Map jobj0b2 = lm.jsonLike.parseOut("{hello: world}").getX();
		
		Map jobj1a = lm.jsonLike.parseOut("{\"Start Date\":month}").getX();
		Map jobj1b = lm.jsonLike.parseOut("{name:\"SF exported csv\"}").getX();
		
		Map jobj1 = lm.jsonLike.parseOut("{\"Start Date\":month, name:\"SF exported csv\"}").getX();
		
	}
	

	@Test
	public void testUrl() {
		Lang lang = new Lang();
		LangMisc lm = lang.langMisc;
		lm.urlOrFile.parseOut("https://bbc.co.uk");
		lm.urlOrFile.parseOut("file:///home/daniel/winterwell/moneyscript/data/SF-won-report.csv");
	}
		
	@Test
	public void testStartEnd() {
		Lang lang = new Lang();
		LangMisc lm = lang.langMisc;
		{
			ParseResult<Settings> p = lm.startEndSetting.parseOut("start: March 2012");
			Settings settings = p.getX();
			assert settings.getStart().equals(new Time(2012, 3, 1)) : settings.getStart();
		}
		{
			ParseResult<Settings> p = lm.startEndSetting.parseOut("start: Feb 2013");
			Settings settings = p.getX();
			assert settings.getStart().equals(new Time(2013, 2, 1)) : settings.getStart();
		}
		{
			ParseResult<Settings> p = lm.startEndSetting.parseOut("end: March 2099");
			Settings settings = p.getX();
			settings.setStart(new Time(2019, 2, 1));
			System.out.println(settings.getEnd());
			System.out.println(settings.getRunTime());
			assert new Dt(3, TUnit.YEAR).isShorterThan(settings.getRunTime()) : settings.getRunTime();
		}
		{
			ParseResult<Settings> p = lm.planSettings.parseOut("start: Jan 2020");
			Settings settings = p.getX();
			System.out.println(settings.getStart());			
			assert settings.getStart().equals(new Time(2020,1,1));
		}
	}

	@Test
	public void testColumns() {
		Lang lang = new Lang();
		LangMisc lm = lang.langMisc;
		{
			ParseResult<Settings> p = lm.columnSettings.parseOut("columns: 6 months");
			Settings settings = p.getX();
			Printer.out(settings.getRunTime());
		}
		{
			ParseResult<Settings> p = lm.planSettings.parseOut("columns: 6 months");
			Settings settings = p.getX();
			Printer.out(settings.getRunTime());
		}
	}

	@Test
	public void testImportOver() {
		Lang lang = new Lang();
		LangMisc lm = lang.langMisc;
		{
			ParseResult<ImportCommand> p = lm.importCommand.parseOut("import: http://example.com/asheet.csv");
			ImportCommand settings = p.getX();
			assert settings.src.equals("http://example.com/asheet.csv");
		}
	}
	

	@Test
	public void testImportWithJsonInfo() {
		Lang lang = new Lang();
		LangMisc lm = lang.langMisc;
		{
			ParseResult<ImportCommand> p = lm.importCommand.parseOut("import: http://example.com/asheet.csv {wibble: https://foo.com}");
			ImportCommand settings = p.getX();
			assert settings.src.equals("http://example.com/asheet.csv");
		}
	}
	
	@Test
	public void testStartEndApplied() {
		Lang lang = new Lang();
		Business b = lang.parse("start: March 2012\nend: May 2013");
		Settings settings = b.getSettings();
		Time s = new Time(2012, 3, 1);
		assert settings.getStart().equals(s) : settings.getStart();
		Time e = TimeUtils.getEndOfMonth(new Time(2013, 5, 1));
		assert Math.abs(settings.getEnd().diff(e)) <= TUnit.MINUTE.millisecs : settings.getEnd();
		assert Math.abs(settings.getRunTime().getMillisecs() - s.dt(e).getMillisecs()) <= TUnit.MINUTE.millisecs;
	}

}

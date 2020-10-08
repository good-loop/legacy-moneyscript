package com.winterwell.moneyscript.lang;

import static org.junit.Assert.*;

import org.junit.Test;

import com.winterwell.moneyscript.output.Business;
import com.winterwell.nlp.simpleparser.ParseResult;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;

public class LangMiscTest {

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
			settings._start = new Time(2019, 2, 1);
			System.out.println(settings.getEnd());
			System.out.println(settings.getRunTime());
			assert new Dt(3, TUnit.YEAR).isShorterThan(settings.getRunTime()) : settings.getRunTime();
		}
	}


	@Test
	public void testImportOver() {
		Lang lang = new Lang();
		LangMisc lm = lang.langMisc;
		{
			ParseResult<ImportCommand> p = lm.importCommand.parseOut("import from http://example.com/asheet.csv");
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
		Time e = new Time(2013, 5, 1);
		assert settings.getEnd().equals(e) : settings.getEnd();
		assert settings.getRunTime().equals(s.dt(e));
	}

}

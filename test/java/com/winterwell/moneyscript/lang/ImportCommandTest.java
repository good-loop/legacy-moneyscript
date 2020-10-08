package com.winterwell.moneyscript.lang;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

import com.winterwell.moneyscript.output.Business;
import com.winterwell.utils.time.Time;

public class ImportCommandTest {

	@Test
	public void testBusinessRun() {
		ImportCommand ic = new ImportCommand();
		ic.overwrite = true;
		ic.src = new File("test/test-input.csv").toURI().toString();
		Business bs = new Business();
		bs.getSettings()._start = new Time(new Time().getYear(), 1, 1);
		bs.getSettings()._end = new Time(new Time().getYear(), 12, 31);
		bs.addImportCommand(ic);
		bs.run();
		System.out.println(bs.toCSV());
		System.out.println(bs.toJSON());
	}

}

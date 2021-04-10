package com.winterwell.moneyscript.output;

import java.io.File;

import org.junit.Test;

import com.winterwell.moneyscript.lang.Lang;
import com.winterwell.nlp.simpleparser.Parser;
import com.winterwell.utils.Printer;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.LogFile;

public class SpeedTest {

	LogFile lf = new LogFile();
	
	@Test
	public void testRunGLPlan() {
		
		Parser.DEBUG = false;
		String txt = FileUtils.read(new File("plans/gl2020-2021.ms"));		
		Lang lang = new Lang();
		for(int i=0; i<100; i++) {
			Business b = lang.parse(txt);
			b.run();
			Printer.out(b.toString());
			Printer.out(b.toCSV());
		}
	}

}

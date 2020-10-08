package com.winterwell.moneyscript.output;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;

import org.junit.Test;

import com.winterwell.moneyscript.lang.Lang;
import com.winterwell.moneyscript.lang.Rule;
import com.winterwell.moneyscript.lang.cells.RowName;
import com.winterwell.moneyscript.lang.num.Formula;
import com.winterwell.moneyscript.lang.num.Numerical;
import com.winterwell.nlp.simpleparser.ParseExceptions;
import com.winterwell.nlp.simpleparser.ParseFail;
import com.winterwell.utils.Printer;
import com.winterwell.utils.io.FileUtils;

public class GLTest {

	@Test
	public void test() {
		try {
			Lang lang = new Lang();
			String glplan = FileUtils.read(new File("test/gl.txt"));
			Business b = lang.parse(glplan);
			b.run();
			String csv = b.toCSV();
			Printer.out(csv);
		} catch(ParseExceptions pexs) {
			for(ParseFail pf : pexs.getErrors()) {
				System.out.println(pf.lineNum+" "+pf.slice+"\t"+pf.getMessage());
			}
			throw pexs;
		}

	}

}

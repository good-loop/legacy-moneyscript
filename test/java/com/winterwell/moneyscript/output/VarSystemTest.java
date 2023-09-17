package com.winterwell.moneyscript.output;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import com.winterwell.moneyscript.lang.Lang;
import com.winterwell.utils.containers.Containers;

public class VarSystemTest {

	@Test
	public void testRowExpansion() {
		String s = "start: Jan 2000\nend: Jan 2000\n"
				+"Price [Region=UK]: £1\n"
				+"Price [Region=US]: £2\n"
				+"Revenue: 10 * Price\n";
		Lang lang = new Lang();
		Business b = lang.parse(s);
		List<Row> rows = b.getRows();
		List<String> names = Containers.apply(rows,  Row::getName);
		assert names.size() == 4;
		System.out.println(names);
		assert names.toString().equals(
				"[Price [Region=UK], Price [Region=US], Revenue [Region=UK], Revenue [Region=US]]") : names.toString();		
	}

	@Test
	public void testRunRowExpansion() {
		String s = "start: Jan 2000\nend: Jan 2000\n"
				+"Price [Region=UK]: £1\n"
				+"Price [Region=US]: £2\n"
				+"Revenue: 10 * Price\n";
		Lang lang = new Lang();
		Business b = lang.parse(s);
		b.run();
		
		System.out.println(b.getRows());
		
		String csv = b.toCSV();
		assert csv.contains("Revenue [Region=UK], £10") : csv;
		assert csv.contains("Revenue [Region=US], £20") : csv;
	}
}

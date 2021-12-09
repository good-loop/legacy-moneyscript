package com.winterwell.moneyscript.lang;

import com.winterwell.moneyscript.lang.num.Numerical;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Cell;

public class CompareCommand extends ImportCommand {


	public CompareCommand(String src) {
		super(src);
	}

	@Override
	Numerical run2_setCellValue(Business b, double v, Cell cell) {
		if (cell.row.getName().contains("DEBUGGrant")) {
			System.out.println(cell);
		}
		Numerical ours = b.getCellValue(cell);
		if (ours==null) {
			ours = new Numerical(0);
			b.state.set(cell, ours);
		}
		double d = ours.doubleValue() - v;
		ours.setDelta(d); 
		return ours;
	}
}

package com.winterwell.moneyscript.lang;

import com.winterwell.moneyscript.lang.num.Numerical;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Cell;

public class CompareCommand extends ImportCommand {


	public CompareCommand(String src) {
		super(src);
		blankIsZero = true;
	}
	
	@Override
	void run2_row(String[] row, Business b) {
		super.run2_row(row, b);
	}

	@Override
	Numerical run2_setCellValue(Business b, double v, Cell cell, String srcRowNameIgnored) {
		if (cell.row.getName().contains("Ross") 
				&& cell.toString().contains("2023")) {
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

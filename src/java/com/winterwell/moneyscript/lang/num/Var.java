package com.winterwell.moneyscript.lang.num;

import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Col;
import com.winterwell.moneyscript.output.Row;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.time.TUnit;

public class Var extends Formula {

	private String var;

	public Var(String varName) {
		super("");
		this.var = varName;
	}

	@Override
	public Numerical calculate(Cell b) {
		if (var.equals("row")) {
			// row index ??is this ever useful?
			Row row = b.getRow();
			return new Numerical(b.getBusiness().getRowIndex(row));
		}
		if (var.equals("column")) return new Numerical(b.col.index);
		if (var.equals("month")) {
			assert b.getBusiness().getTimeStep().equals(TUnit.MONTH.dt);
			return new Numerical(b.col.index);
		}		
		if (var.equals("year")) {
			assert b.getBusiness().getTimeStep().equals(TUnit.MONTH.dt);
			int yr = 1 + (int)Math.floor(b.getColumn().index/12);
			return new Numerical(yr);
		}
		// Note that previous(X) is handled by UnaryOp
		if (var.equals("previous")) {
			if (b.getColumn().index == 1) return new Numerical(0);
			Cell prevCell = new Cell(b.getRow(), new Col(b.getColumn().index-1));
			return b.getBusiness().getCellValue(prevCell);
		}
		throw new TodoException(var);
	}

}
package com.winterwell.moneyscript.lang.num;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.winterwell.moneyscript.lang.cells.CellSet;
import com.winterwell.moneyscript.lang.cells.CurrentRow;
import com.winterwell.moneyscript.lang.cells.RowName;
import com.winterwell.moneyscript.lang.cells.Scenario;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Containers;

/**
 * Just a number!
 * @author daniel
 *
 */
public class BasicFormula extends Formula {

	public boolean isStacked() {
		return sel instanceof CurrentRow;
	}
	
	public CellSet getCellSetSelector() {
		return sel;
	}
	
	public Set<String> getRowNames(Cell focus) {
		return (Set) (sel==null? Collections.emptySet() : sel.getRowNames(focus));
	}
	
	CellSet sel;
	Numerical num;

	@Override
	public String toString() {
		return num==null? sel.toString() : num.toString();
	}
	
	public BasicFormula(Numerical num) {
		super("");
		this.num = num;
	}
	public BasicFormula(CellSet sel) {
		super("");
		this.sel = sel;
	}
	
	@Override
	public Numerical calculate(Cell b) {		 
		assert Utils.isBlank(op) : op;
//		assert b != null;
		if (num!=null) return sample(num);
		// HACK: a scenario?
		if (sel instanceof RowName) {
			Scenario s = new Scenario(((RowName) sel).getRowName());
			Boolean onOff = b.getBusiness().getScenarios().get(s);
			if (onOff != null) {
				if (onOff) {
					return new Numerical(1);					
				}
				return new Numerical(0);
			}			
		}
		// get first cell of the set
		Collection<Cell> cell2 = sel.getCells(b, false);
		if (cell2==null || cell2.isEmpty()) {
			return null;
		}
		assert cell2.size() == 1 : sel+" "+cell2;
		Cell cell = Containers.first(cell2);
//		// special case: group rows <- nah just eval - handled in Business
//		if (cell.row.isGroup()) {
//			return cell.row.getGroupValue(cell.col, b);
//		}
		// Get the cell value - this can trigger a further evaluate 
		Numerical n = b.getBusiness().getCellValue(cell);
		return n;
	}

	public boolean isCurrentRow() {
		return sel instanceof CurrentRow;
	}
	
}


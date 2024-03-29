package com.winterwell.moneyscript.lang.num;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.winterwell.moneyscript.lang.cells.CellSet;
import com.winterwell.moneyscript.lang.cells.CurrentRow;
import com.winterwell.moneyscript.lang.cells.RowName;
import com.winterwell.moneyscript.lang.cells.Scenario;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Row;
import com.winterwell.moneyscript.output.RowVar;
import com.winterwell.moneyscript.output.VarSystem;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.containers.Tree;
import com.winterwell.utils.time.Time;

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
	private String tag;

	public String getTag() {
		return tag;
	}
	
	@Override
	public String toString() {
		return StrUtils.joinWithSkip("", num==null? sel.toString() : num.toString(), getTag());
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
//		String sdebug = b+"";
//		if(sdebug.contains("Donations1")) {
//			Utils.breakpoint();
//		}
		assert Utils.isBlank(op) : op;
//		assert b != null;
		if (num!=null) {
			Numerical n = sample(num);
			if (tag!=null) {
				n = n.getTagged(tag);
			}
			n = fx(n, b);
			return n;
		}
		// HACK: a scenario? TODO cache
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
			if (sel instanceof RowName) {
				VarSystem vs = b.getBusiness().getVars();
				List<RowVar> svs = vs.getCurrentSetVars();
				sel.getCells(b, false); // debug
				throw new IllegalStateException("No active cells for "+sel+" context:"+b);
			}
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
		// filter by tag?
		if (tag!=null) {
			String _tag = LangNum.resolveTag(tag, b);
			n = n.getTagged(_tag);
		}
		return n;
	}

	/**
	 * Currency conversion FX
	 * @param n
	 * @param b
	 * @return
	 */
	private Numerical fx(Numerical n, Cell b) {
		// HACK: handle dollars if a convertor was set for that
		if ("$".equals(n.getUnit())) {
			// did the user specify?
			Row gbpusd = b.getBusiness().getRow("Set GBP USD"); // HACK!!
			double v2;
			if (gbpusd != null && b != null && ! gbpusd.equals(b.getRow())) {
				Cell rcell = new Cell(gbpusd, b.getColumn());
				Numerical rval = b.getBusiness().getCellValue(rcell);
				v2 = n.doubleValue() / rval.doubleValue();
			} else {
				// use the date
				Time date = b==null? new Time() : b.getColumn().getTime();
				CurrencyConvertor_USD2GBP cc = new CurrencyConvertor_USD2GBP(date);				
				double v = n.doubleValue();
				v2 = cc.convertES(v);
			}
			Numerical n2 = new Numerical(v2);
			n2.setUnit("£");
			return n2;
		}
		// TODO other currencies
		return n;
	}

	public boolean isCurrentRow() {
		return sel instanceof CurrentRow;
	}

	public void setTag(String htag) {
		this.tag = htag;
	}

	@Override
	public Tree<Formula> asTree() {
		return new Tree(this);
	}
	
}


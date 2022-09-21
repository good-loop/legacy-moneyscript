package com.winterwell.moneyscript.lang.num;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.winterwell.moneyscript.lang.UncertainNumerical;
import com.winterwell.moneyscript.lang.cells.CellSet;
import com.winterwell.moneyscript.lang.cells.RowName;
import com.winterwell.moneyscript.lang.time.DtDesc;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Col;
import com.winterwell.moneyscript.output.Row;
import com.winterwell.moneyscript.webapp.GSheetFromMS;
import com.winterwell.utils.Dep;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.log.Log;

public class UnaryOp extends Formula {

	public final Formula right;
	
	public UnaryOp(String op, Formula right) {
		super(op);
//		assert ! (right instanceof SetValueFormula);
		this.right = right;
	}
	
	@Override
	public boolean isStacked() {
		return right.isStacked();
	}
	
	@Override
	public Set<String> getRowNames(Cell focus) {
		return right.getRowNames(focus);
	}

	@Override
	public Numerical calculate(Cell b) {
		if (op.startsWith("sum")) {
			return calculate2_sum(b);
		}
		if (op.startsWith("count row")) {
			return calculate2_countRow(b); 		// TODO preserve tags
		}
		if (op.startsWith("count")) {
			return calculate2_count(b);
		}
		// e.g. "previous Debt"
		if ("previous".equals(op)) {
			return calculate2_previous(b); 
		}
		Numerical x = right.calculate(b);
		if (x==null) return null;
		assert ! (x instanceof UncertainNumerical) : this;		
		if (op.equals("round")) { 		// TODO preserve tags
			return new Numerical(Math.round(x.doubleValue()), x.getUnit());			
		}
		if (op.equals("round down")) { 		// TODO preserve tags
			return new Numerical(Math.floor(x.doubleValue()), x.getUnit());			
		}
		if (op.equals("round up")) { 		// TODO preserve tags
			return new Numerical(Math.ceil(x.doubleValue()), x.getUnit());			
		}
		if (op.equals("sqrt")) { 		// TODO preserve tags
			return new Numerical(Math.sqrt(x.doubleValue())); // any unit??
		}
		if (op.equals("log")) { 		// TODO preserve tags
			return new Numerical(Math.log(x.doubleValue())); // any unit??
		}
		// Probability
		if (op.equals("p")) { 		// TODO preserve tags??
			double p = x.doubleValue();
			// hack x% per year = 1 - 12th rt (1 -x) per month
			// So we interpret p(10% per year) as P(at least once within a year) = 10%
			if (right instanceof PerFormula) {
				DtDesc dt = ((PerFormula)right).dt;				
				double n = dt.calculate(b).divide(b.getBusiness().getTimeStep());
				// undo the division
				p = n*p;
				p = 1 - Math.pow(1-p, 1/n);
			}						
			boolean yes = Utils.getRandomChoice(p);
			Numerical n = new Numerical(yes? 1 : 0);
			return n;
		}
		// ?? extract "3" from "3 @ £10" 
//		if (op=="#") {
//			Numerical x = right.calculate(b);
//			if (x==null) return Numerical.NULL;
//			assert x instanceof Numerical2;
//			return ((Numerical2)x).getLhs();
//		}
		// Fail
		throw new TodoException(op+" "+right);
	}

	
	private Numerical calculate2_previous(Cell b) {
		// at the start? 0 then
		if (b.getColumn().index == 1) {
			Numerical n = new Numerical(0);
			// NB: No excel cell reference
			return n;
		}
		// must be a cell set as formula
		CellSet cellSet = ((BasicFormula)right).sel;
		assert cellSet != null : right;
		String rowName;
		if (cellSet instanceof RowName) {
			// shortcut
			rowName = ((RowName) cellSet).getRowName();			
		} else {
			Set<String> rows = cellSet.getRowNames(b);
			rowName = Containers.first(rows);
	//		assert rows.size() == 1 : rows; group rows pull back more
		}
		Business biz = b.getBusiness();
		Row row = biz.getRow(rowName);
		assert row != null : cellSet;
		Cell prevCell = new Cell(row, new Col(b.getColumn().index-1));
//		Cell b2 = new Cell(prevCell);
		if ( ! cellSet.contains(prevCell, b)) {
			return null;
		}			
		Numerical pc = biz.getCellValue(prevCell);
		GSheetFromMS gs = Dep.getWithDefault(GSheetFromMS.class, null);
		if (gs!=null) {
			pc = new Numerical(pc);
			pc.excel = gs.cellRef(prevCell.row, prevCell.col);
		}
		return pc;
	}

	private Numerical calculate2_sum(Cell b) {
		Numerical sum = new Numerical(0);
		// eg "sum Sales"
		if (right instanceof BasicFormula) {
			// right should be a selector
			CellSet sel = ((BasicFormula)right).sel;
			// ?? FIXME sum Sales = sum (Sales from start to now)
//			sel.getStartColumn(sel.get, b);
			Collection<Cell> cells = sel.getCells(b, true);
			for (Cell cell : cells) {
				Numerical c = b.getBusiness().getCellValue(cell);
				sum = sum.plus(c);
			}
			return sum;
		}
		
		Collection<Cell> cells = b.getRow().getCells();
		// apply the op
		for (Cell cell : cells) {
			Numerical c = right.calculate(cell);
			sum = sum.plus(c);
		}		
		return sum;
	}


	/**
	 * count of non-zero values __in a column__
	 * Unpacks groups 
	 * @param b
	 * @return
	 */
	private Numerical calculate2_count(Cell b) {
		// eg "sum Sales"
		// right should be a selector
		CellSet sel = ((BasicFormula)right).sel;
		// get the rows
		List<String> rns = new ArrayList(sel.getRowNames(b));
		ArrayList<Row> leafRows = new ArrayList(); 
		Business biz = b.getBusiness();
		getLeafRows(Containers.apply(rns, biz::getRow), leafRows, biz);
		
		// apply the op
		int cnt = 0;
		Map<String,Double> cnt4tag = new ArrayMap();
		for(Row row : leafRows) {
			Cell rcell = new Cell(row, b.getColumn());
			Numerical c = biz.getCellValue(rcell);
			if (c == null || c.doubleValue() == 0) continue;
			cnt++;
			if (c.value4tag!=null) {
				for(Map.Entry<String,Double> e : c.value4tag.entrySet()) {
					Double ce = cnt4tag.get(e.getKey());
					double ce2 = ce==null? 1 : ce + 1;
					cnt4tag.put(e.getKey(), ce2);
				}
			}
		}
		Numerical n = new Numerical(cnt);
		// NB: there's no unit carried over for a count
		if ( ! cnt4tag.isEmpty()) {
			n.value4tag = cnt4tag;
		}
		return n;
	}



	/**
	 * count of non-zero values __in a row__
	 * Unpacks groups 
	 * @param b
	 * @return
	 */
	private Numerical calculate2_countRow(Cell b) {
		// eg "sum Sales"
		// right should be a selector
		CellSet sel = ((BasicFormula)right).sel;
		
		// Which row? Probably b.row...
		Row row = b.row;
		// ...get the leaf rows - e.g. to handle "count row(MySpecificRow)"
		ArrayList<Row> leafRows = new ArrayList();
		List<String> rns = new ArrayList(sel.getRowNames(b));		 
		Business biz = b.getBusiness();
		List<Row> rows = Containers.apply(rns, biz::getRow);
		getLeafRows(rows, leafRows, biz);		
		if ( ! leafRows.contains(b.row)) {
			if (leafRows.size() != 1) {
				throw new CalculateException(this+" for "+b+": which row is ambiguous: "+leafRows);
			}
			row = leafRows.get(0);
		}
		
		// get all the cells in the selector
		Collection<Cell> cells = sel.getCells(b, true);
		// filter by row and by non-zero		
		int cnt = 0;
		for (Cell cell : cells) {
			if ( ! leafRows.contains(cell.row)) {
				continue;
			}
			Numerical c = biz.getCellValue(cell);
			if (c != null && c.doubleValue() != 0) {
				cnt++;
			}
		}
		return new Numerical(cnt);
	}


	/**
	 * Recursive expand of the agenda, adding to leafRows
	 * @param agenda
	 * @param leafRows
	 * @param b
	 */
	private void getLeafRows(List<Row> agenda, ArrayList leafRows, Business b) {
		for(Row row : agenda) {
			if (row==null) {
				Log.w("UnaryOp "+this, "skipping null row in getLeafRows()");
				continue;
			}
			if (row.isGroup()) {
				List<Row> kids = row.getChildren();
				getLeafRows(kids, leafRows, b);
			} else {
				// NB: dupes can happen if the input list contains e.g. a group + a member of that group
				if ( ! leafRows.contains(row)) {
					leafRows.add(row);
				}
			}
		}		
	}
	

	@Override
	public String toString() {
		return op+" "+right;
	}
}
package com.winterwell.moneyscript.lang.cells;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.winterwell.moneyscript.lang.Lang;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.BusinessContext;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Col;
import com.winterwell.moneyscript.output.Row;
import com.winterwell.moneyscript.output.RowVar;
import com.winterwell.moneyscript.output.VarSystem;
import com.winterwell.nlp.simpleparser.ParseResult;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.containers.ArraySet;
import com.winterwell.utils.containers.Containers;

public class RowName extends CellSet {
	private final String rowName;
	
	public RowName(String rowName) {
		super(rowName);
		assert rowName != null;
//		assert LangCellSet.rowName.parse(rowName) != null : rowName;
		this.rowName = rowName;
	}

	Collection<SetVariable> vars;
	/**
	 * cache for RowName = a row. i.e. no SetVariables
	 */
//	private transient Row rowSimple;
	
	@Override
	public Collection<SetVariable> getVars(Cell cell) {
//		if (vars!=null && vars.isEmpty()) {
//			return vars;
//		}
		VarSystem vs = Business.get().getVars();
		if ( ! vs.isSwitchRow(rowName)) {
			vars = Collections.EMPTY_LIST;
			return vars;
		}
		// HACK a 2nd order switch row
		String cellRowName = cell.row.getName();
		LangCellSet lcs = new LangCellSet();
		ParseResult<RowNameWithFixedVariables> p = lcs.rowNameWithFixedVariable.parse(cellRowName);
		RowNameWithFixedVariables rnv = p.getX();
//		vars = rnv.vars; // can't stash this as this RowName can cover several expanded rows with different values
		return rnv.vars;
	}

	public String getRowName() {
		return rowName;
	}
	
	@Override
	public Set<String> getRowNames(Cell focus) {
		// expand a group?
		Row row = Cell.getBusiness().getRow(rowName); 
		if (row != null && row.isGroup()) { // cache??
			List<Row> rows = row.flatten();			
			List<String> names = Containers.apply(rows, r -> r.getName());
			return new ArraySet(names);
		}
		return Collections.singleton(rowName);
	}
	
	@Override
	public String toString() {
		return rowName;
	}
	@Override
	public boolean contains(Cell cell, Cell context) {
		Row myRow = getRow(context);
		return contains2(myRow, cell);
	}
	
	private Row getRow(Cell ignored) {
//		if (rowSimple!=null) return rowSimple;
		Business b = BusinessContext.getBusiness();
		Row r = b.getRow(rowName);
		if (r != null) {
			assert ! b.getVars().isSwitchRow(rowName) : rowName;
//			rowSimple = r;
			return r;
		}
		// Is it an object using a variable eg [Region in Region Mix * Region.Price]?
		// HACK only handles one deep -- not recursive
		VarSystem vs = b.getVars();
		if (rowName.contains(".")) {
			String activeName = vs.getActiveName(rowName);
			Row modRow = b.getRow(activeName);
			if (modRow !=null) {
				return modRow;			
			}
		}
		// Is it an unfixed switch row?
		if (vs.isSwitchRow(rowName)) {
			String rn = vs.getActiveRow(rowName);
			Row aRow = b.getRow(rn);
			return aRow;
		}
		return null;
	}

	/**
	 * 
	 * @param myRow
	 * @param cell
	 * @return true if cell is in myRow or its children
	 */
	private boolean contains2(Row myRow, Cell cell) {
		if (cell.row == myRow) return true;
		if ( ! myRow.isGroup()) return false;
		// NB: Profiling showed this as a bottleneck, Jan 2022
		if (_subRows==null) {
			_subRows = new HashSet();
			initSubRows(myRow);
		}
		boolean in = _subRows.contains(cell.row.getName());
		return in;
	}		
	
	private void initSubRows(Row myRow) {		
		for(Row kRow : myRow.getChildren()) {
			_subRows.add(kRow.getName());
			initSubRows(kRow);
		}			
	}

	transient Set<String> _subRows;
	
	
	
	
	/**
	 * the equivalent cell in this row. e.g. for rows A, B    
	 * then RowName("A").getCells(context: {row:B, col:1}) = {row:A, col:1}
	 * 
	 *  Use-case: e.g. formulae like "B: 2 * A"
	 *  @return HACK: null if the row-name is invalid
	 */
	@Override
	public Collection<Cell> getCells(Cell bc, boolean wide) {
		Row row = getRow(null);
		if (row==null) {
			// HACK ?? Why not throw an exception??
			return null;
		}
		assert row != null : rowName+" in "+bc;
		if (wide) {
			return row.getCells();
		}
		Col col = bc.getColumn();
		Cell cell = new Cell(row, col);
		return Collections.singletonList(cell);
	}
}


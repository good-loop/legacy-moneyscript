package com.winterwell.moneyscript.lang.cells;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Col;
import com.winterwell.moneyscript.output.Row;

/**
 * 
 * Note: this class has a natural ordering based on specificity (a bit like css) 
 * that is inconsistent with equals.
 * @author daniel
 *
 */
public abstract class CellSet implements Comparable<CellSet> {

	/**
	 * Can be null
	 */
	private transient String src;
	
	@Override
	public String toString() {
		return getClass().getSimpleName()+"["+src+"]";
	}
	
	public CellSet(String src) {
		this.src = src;
	}		
	
	/** 
	 * Do the work for filter (note: calling filter directly can be more efficient).
	 * @param row
	 * @param col
	 * @return true if this Filter says yes to row/col
	 */
	public abstract boolean contains(Cell cell, Cell bc);

	/** 
	 * @param focus
	 * @param wideView if true, return a wide interpretation, e.g. 
	 * 	"Sales" -> all cells in the Sales row, from start to end.
	 * @return cell or cells to use in formulae, given the focus. Typically just one cell!
	 * e.g. for A: B + C, then the cellset `B` would return focus -> {row:B, col:focus.col}
	 */
	public abstract Collection<Cell> getCells(Cell focus, boolean wideView);
	

	/**
	 * @return all the rows which might contain cells.
	 */
	public abstract Set<String> getRowNames(Cell focus);	
	

	public final Col getStartColumn(Row row, Cell context) {
		for(Cell cell : row.getCells()) {
			if (contains(cell, context)) return cell.col;
		}
		return Col.THE_INDEFINITE_FUTURE;
	}

	/**
	 * Compare to selectors to determine which is more specific
	 * and hence gets priority. The most specific should come last
	 * in a sort.
	 * <p>
	 * Note: this class has a natural ordering that is inconsistent with equals.
	 * This is a partial ordering -- most selectors come out
	 * equally specific.
	 * 
	 * @param bs
	 * @return
	 */
	public int compareTo(CellSet other) {
		Class<? extends CellSet> k = getClass();
		Class<? extends CellSet> k2 = other.getClass();
		int ik = sortOrder.indexOf(k);
		int ik2 = sortOrder.indexOf(k2);
		return ik - ik2;
	}
	static List<Class> sortOrder = Arrays.asList(
			(Class)AllCellSet.class, Union.class, RowName.class, FilteredCellSet.class
	);

	public String getSrc() {
		return src;
	}

}

package com.winterwell.moneyscript.lang.cells;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.winterwell.moneyscript.output.Cell;
import com.winterwell.utils.containers.ArraySet;

/**
 * AND two cell sets
 * @author daniel
 *
 */
public class IntersectionCellSet extends CellSet {
	
	private CellSet a;
	private CellSet b;
	

	@Override
	public String toString() {
		return "IntersectionCellSet [a=" + a + ", b=" + b + "]";
	}

	public IntersectionCellSet(CellSet a, CellSet b, String src) {
		super(src);
		this.a = a;
		this.b = b;
		assert a != null && b != null;
	}

	@Override
	public boolean contains(Cell cell, Cell context) {
		return a.contains(cell, context) && b.contains(cell, context);
	}
	
	@Override
	public Set<String> getRowNames(Cell focus) {
		Set<String> arns = a.getRowNames(focus);
		Set<String> brns = b.getRowNames(focus);
		Set<String> rn = new ArraySet<String>(arns);
		rn.retainAll(brns);
		return rn;
	}

	@Override
	public Collection<Cell> getCells(Cell bc, boolean wide) {
		Collection<Cell> ac = a.getCells(bc, wide);
		Collection<Cell> bs = b.getCells(bc, wide);
		HashSet<Cell> cells = new HashSet<Cell>(ac);
		cells.retainAll(bs);
		return cells;
	}

	/**
	 * 
	 * @param a can be null
	 * @param b can be null
	 * @return intersection (and) of a, b
	 */
	public static CellSet make(CellSet a, CellSet b) {
		if (a==null) return b;
		if (b==null) return a;
		if (a.equals(b)) {
			return a;
		}
		return new IntersectionCellSet(a, b, a.getSrc()+" and "+b.getSrc());
	}

}

package com.winterwell.moneyscript.lang.cells;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.utils.containers.ArraySet;
import com.winterwell.utils.containers.Containers;

/**
 * OR two cell sets
 * @author daniel
 *
 */
public class Union extends CellSet {
	
	private CellSet a;
	private CellSet b;
	

	public Union(CellSet a, CellSet b) {
		this.a = a;
		this.b = b;
		assert a != null && b != null;
	}

	@Override
	public boolean contains(Cell cell, Cell context) {
		return a.contains(cell, context) || b.contains(cell, context);
	}
	
	@Override
	public Set<String> getRowNames() {
		Set<String> rn = new ArraySet<String>(a.getRowNames());
		rn.addAll(b.getRowNames());
		return rn;
	}

	@Override
	public Collection<Cell> getCells(Cell bc, boolean wide) {
		Collection<Cell> ac = a.getCells(bc, wide);
		Collection<Cell> bs = b.getCells(bc, wide);
		HashSet<Cell> cells = new HashSet<Cell>(ac);
		cells.addAll(bs);
		return cells;
	}

}

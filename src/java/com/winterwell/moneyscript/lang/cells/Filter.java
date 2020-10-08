package com.winterwell.moneyscript.lang.cells;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.winterwell.moneyscript.output.Cell;
import com.winterwell.nlp.simpleparser.AST;

public abstract class Filter {
	
	public enum KDirn {
		LEFT, UP, RIGHT, HERE
	}

	protected KDirn dirn;

	@Override
	public String toString() {
		return getClass().getSimpleName()+"["+op+"]";
	}
	/**
	 * @return Can be null
	 */
	public KDirn getDirection() {
		return dirn;
	}

	private AST tree;
	protected String op;

	public Filter() {
		this(null);
	}
	
	public Filter(AST tree) {
		this.tree = tree;
		if (tree!=null) {
			op = tree.getValue().toString();
		}
	}

	/** 
	 * Do the work for filter (note: calling filter directly can be more efficient).
	 * 
	 * TODO is this correct for chaining filters?? e.g. "from month 3 for 2 months"
	 * 
	 * @param cell
	 * @param context The focus cell (which can sometimes affect cellset meaning)
	 * @return true if this Filter says yes to row/col
	 */
	public abstract boolean contains(Cell cell, Cell context);

	public Collection<Cell> filter(Collection<Cell> cells, Cell context) {
		List<Cell> list = new ArrayList<Cell>();
		for (Cell cell : cells) {
			if ( ! contains(cell, context)) continue;
			list.add(cell);
		}
		return list;
	}
	
	
}

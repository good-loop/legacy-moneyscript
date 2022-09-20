package com.winterwell.moneyscript.lang.cells;

import java.util.Collection;
import java.util.List;

import com.winterwell.moneyscript.output.Cell;
import com.winterwell.utils.Printer;

/**
 * Chain filters together AND
 * @author daniel
 *
 */
public class ChainFilter extends Filter {
		
	private List<Filter> filters;

	@Override
	public String toString() {
		return Printer.toString(filters, " && ");
	}
	
	public ChainFilter(List<Filter> filters) {
		super(null);
		this.filters = filters;
		for (Filter f : filters) {
			if (dirn==null) dirn = f.dirn; 
		}
	}

	@Override
	public Collection<Cell> filter(Collection<Cell> cells, Cell context) {
		for (Filter f : filters) {
			cells = f.filter(cells, context);
		}
		return cells;
	}

	@Override
	public boolean contains(Cell cell, Cell context) {
		// Hm... not always correct!
		for (Filter f : filters) {
			if ( ! f.contains(cell, context)) {
				return false;
			}
		}
		return true;
	}		
}
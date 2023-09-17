package com.winterwell.moneyscript.lang.cells;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Cell;

public class RowNameWithFixedVariables extends RowName {

	Collection<SetVariable> vars;
	private String baseName;

	public RowNameWithFixedVariables(String parsed, String baseName, Collection<SetVariable> vs) {
		super(parsed); // rowName includes the [A=B] bits
		// Because Price [Region=US] and Price [Region=UK] need to be different rows in the output spreadsheet
		this.baseName = baseName;
		vars = vs;
	}

	@Override
	public boolean contains(Cell cell, Cell context) {
		// Is this needed?? the key thing is e.g. connecting a reference to"Price" with the row "Price [Region=UK]"
		// vars filter
		Business biz = cell.getBusiness();
		for(SetVariable sv : vars) {
			if ( ! sv.isTrue(biz)) {
				return false;
			}
		}
		return super.contains(cell, context);
	}
	
	@Override
	public String toString() {
		return getRowName()+vars;
	}
}

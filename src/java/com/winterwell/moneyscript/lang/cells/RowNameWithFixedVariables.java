package com.winterwell.moneyscript.lang.cells;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.winterwell.moneyscript.output.Cell;

public class RowNameWithFixedVariables extends RowName {

	Collection<SetVariable> vars;

	public RowNameWithFixedVariables(String rowName, Collection<SetVariable> vs) {
		super(rowName);
		vars = vs;
	}

	@Override
	public String toString() {
		return getRowName()+vars;
	}
}

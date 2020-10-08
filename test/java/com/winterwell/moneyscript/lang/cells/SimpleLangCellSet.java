package com.winterwell.moneyscript.lang.cells;

import com.winterwell.moneyscript.lang.cells.CellSet;
import com.winterwell.moneyscript.lang.cells.LangCellSet;
import com.winterwell.moneyscript.lang.cells.RowName;
import com.winterwell.nlp.simpleparser.PP;
import com.winterwell.nlp.simpleparser.ParseFail;
import com.winterwell.nlp.simpleparser.ParseResult;

public class SimpleLangCellSet extends LangCellSet {

	public SimpleLangCellSet() {
		// replace the cell set with something simpler
		new PP<CellSet>(rowName) {
			@Override
			protected CellSet process(ParseResult<?> r) throws ParseFail {
				return new RowName(r.parsed());
			}			
		}.label(cellSet.getName());
	}
	
}

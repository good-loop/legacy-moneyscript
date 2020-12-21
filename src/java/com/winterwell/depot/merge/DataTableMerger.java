
package com.winterwell.depot.merge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.winterwell.maths.datastorage.DataTable;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.CSVReader;

/**
 * Diff and merge for lists.
 * @testedby  DataTableMergerTest
 * @author daniel
 */
public class DataTableMerger<X> extends AMerger<DataTable> {

	private static final Object NULL = "null";

	public DataTableMerger(Merger merger) {
		super(merger);			
	}
	
	boolean overlapOnly = true;

	@Override
	public Diff diff(DataTable before, DataTable after) {		
		
		List<Object[]> rowsBefore = Containers.getList(before);
		List<Object[]> rowsAfter = Containers.getList(after);
		List<Diff[]> diffs = new ArrayList();
		int ar = 0, br = 0;
		Map<String,Integer> bcol12row = before.getColumn1toRow();
		for(Object[] arow : rowsAfter) {
			ar++; 
			String rowName = String.valueOf(arow[0]);
			Integer bi = Containers.getLenient(bcol12row, rowName);
			if (bi==null) {
				assert overlapOnly : "TODO";
				continue;
			}
			Object[] brow = rowsBefore.get(bi);
			// TODO align rows for differing columns
			int min = Math.min(arow.length, brow.length);
			Diff[] rowDiffs = new Diff[min];
			for(int i=0; i<min; i++) {
				Object bv = brow[i]; 
				Object av = arow[i];				
				// convert
				double bvn = MathUtils.getNumber(bv);
				double avn = MathUtils.getNumber(av);
				
				Diff di = recursiveMerger.diff(bvn, avn);
				rowDiffs[i] = di;
			}
			diffs.add(rowDiffs);
			br++;
		}
		Diff _diff = new Diff<List<Diff[]>>(DataTableMerger.class, diffs);
		return _diff;
	}

	@Override
	public DataTable applyDiff(DataTable a, Diff diff) {
		throw new UnsupportedOperationException("TODO");
	}

	@Override
	public DataTable stripDiffs(DataTable v) {
		throw new UnsupportedOperationException();
	}

}

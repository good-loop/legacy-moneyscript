
package com.winterwell.depot.merge;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

import com.winterwell.depot.merge.DataTableMerger;
import com.winterwell.depot.merge.Merger;
import com.winterwell.maths.datastorage.DataTable;
import com.winterwell.utils.io.CSVReader;
import com.winterwell.utils.io.ISerialize;


public class DataTableMergerTest {

	@Test
	public void testDiff() {
		DataTableMerger m = new DataTableMerger<>(new Merger());
		m.overlapOnly = true;
				
		File f = new File("/home/daniel/Downloads/Copy of Reasonable Estimate_Q2 Revised Financial Model & Forecast_29.07.20_LIVE - P&L.csv");
		assert f.isFile();
		// 
		File f2 = new File("/home/daniel/Downloads/2021 Budget Plan.csv");
		assert f2.isFile();
		
		CSVReader c1 = new CSVReader(f);
		CSVReader c2 = new CSVReader(f2);
		
		// hm - no, we want each cell
		ISerialize[] serialisers = new ISerialize[] {
				
		};
		DataTable<String> d1 = new DataTable<>(c1, serialisers);
		DataTable<String> d2 = new DataTable<>(c2, serialisers);
		
		// remove some columns
		Object[] row0 = d1.getRow(0);
		Object[] row1 = d1.getRow(1);
//		d1.removeColumn(0);
		
		Diff diff = m.diff(d1, d2);
		System.out.println(diff);
	}

}

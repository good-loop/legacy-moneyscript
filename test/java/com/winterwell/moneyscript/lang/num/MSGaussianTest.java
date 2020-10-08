package com.winterwell.moneyscript.lang.num;

import java.io.File;
import java.util.Map;

import org.junit.Test;

import com.winterwell.maths.graph.RenderGraph_InfoVisJs;
import com.winterwell.maths.stats.StatsUtils;
import com.winterwell.maths.stats.distributions.d1.Gaussian1D;
import com.winterwell.maths.stats.distributions.d1.HistogramData;
import com.winterwell.maths.stats.distributions.d1.MeanVar1D;
import com.winterwell.maths.stats.distributions.discrete.ObjectDistribution;
import com.winterwell.moneyscript.lang.ImportCommand;
import com.winterwell.nlp.docmodels.WordFreqDocModel;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;
import com.winterwell.utils.io.CSVReader;

public class MSGaussianTest {

	@Test
	public void testSales() {
		// import: plans/SF-report-all-data-2019-2020.csv
//		ImportCommand ic = new ImportCommand();
//		ic.src = 
		File f = new File("plans/SF-report-all-data-2019-2020.csv");		
		CSVReader r = new CSVReader(f);
		r.setHeaders(r.next());
		MeanVar1D mvAll = new MeanVar1D();
		MeanVar1D mvNew = new MeanVar1D();
		MeanVar1D mvExisting = new MeanVar1D();
		ObjectDistribution<String> skips = new ObjectDistribution<String>();
		HistogramData hdNew = new HistogramData(0, 200000, 100);
		HistogramData hdAll = new HistogramData(0, 200000, 100);
		HistogramData hdOld = new HistogramData(0, 200000, 100);
		for (Map<String, String> row : r.asListOfMaps()) {
			String stage = row.get("Stage");
			if ( ! "Closed Won".equals(stage)) {
				skips.train1(stage);
				continue;
			}
			String newOld = row.get("Type");
			double val = MathUtils.getNumber(row.get("Net Amount (- Agency Fee)"));
			if (val==0) {
				// huh?
				skips.train1(row.keySet().toString());
				continue;
			}
			if (val > 150000) {
				skips.train1(row.toString());
				continue;
			}
			if ("New Business".equals(newOld)) {
				mvNew.train1(val);
				hdNew.train1(val);
			} else if ("Existing Business".equals(newOld)) {
				mvExisting.train1(val);
				hdOld.train1(val);
			} else {
				// assume existing
				mvExisting.train1(val);
//				skips.train1(newOld);
			}
			mvAll.train1(val);
			hdAll.train1(val);
			System.out.println(val);
		}
		Printer.out("skips", skips);
		Printer.out("mvAll", mvAll, mvAll.getVariance());
		Printer.out("mvNew", mvNew);
		Printer.out("mvOld", mvExisting, mvExisting.getCount());
		
		// Plot it
		
		// t test of
		StudentsTTest tt = new StudentsTTest(mvNew, mvExisting);
		Printer.out("t", tt.getTStatistic());
	}
}

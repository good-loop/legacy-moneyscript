package com.winterwell.moneyscript.lang;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.winterwell.maths.datastorage.DataTable;
import com.winterwell.moneyscript.lang.cells.CellSet;
import com.winterwell.moneyscript.lang.cells.RowName;
import com.winterwell.moneyscript.lang.num.BasicFormula;
import com.winterwell.moneyscript.lang.num.Formula;
import com.winterwell.moneyscript.lang.num.Numerical;
import com.winterwell.moneyscript.lang.num.UnaryOp;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.BusinessContext;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Col;
import com.winterwell.moneyscript.output.Row;
import com.winterwell.nlp.dict.Dictionary;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.containers.ListMap;
import com.winterwell.utils.io.CSVReader;
import com.winterwell.utils.io.CSVSpec;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.Period;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;

/**
 * 
 * @testedby ImportRowCommandTest
 * @author daniel
 *
 */
public class ImportRowCommand extends ImportCommand {

	private String targetRowName = "dummy";

	public ImportRowCommand(String src) {
		super(src);
	}	

	public void run(Business b) {		
		// Is it another m$ file??
		if (src.endsWith(".m$") || src.endsWith(".ms")) {			
			return; // Should be done already during parse!
		}
		// fetch
		fetch();
		// not a csv?
		if (csv.startsWith("<!doctype ") || csv.startsWith("<html")) {
			throw new IllegalArgumentException("Import fail: Url "+src+" returned a web page NOT a csv");
		}
		// CSV
		CSVSpec spec = new CSVSpec();
		CSVReader r = new CSVReader(new StringReader(csv), spec);
		// the first row MUST be headers
		r.setHeaders(r.next());
		r.setNumFields(-1); // flex
		List<String> headers = r.getHeaders();		

		Iterable<Map<String, String>> items = r.asListOfMaps();
		values = new ArrayList();
		values.add(null); // to make it 1-indexed
		
		// Time column
		String timeCol = run2_pickTimeColumn(r.getHeaders());
		if (timeCol==null) {
			throw new IllegalArgumentException("No month/date/time column recognised in "+src);
		}
		// process the items
		// ...Split by month (column)
		ListMap<Col,Map> col2items = new ListMap<>();
		Period period = new Period(b.getSettings().getStart(), b.getSettings().getEnd());
		for (Map<String, String> map : items) {
			String s = map.get(timeCol);
			Time t = TimeUtils.parseExperimental(s);
			// count [start, end) so we can't double-count
			if (t.isBefore(period.first) || t.isAfterOrEqualTo(period.second)) {
				continue; // skip, outside time window
			}
			Col col = b.getColForTime(t);
			col2items.add(col, map);				
		}
		// calculate -- TODO other than sum e.g. just as-is (though already supported)
		// HACK assume a basic sum, and unpack accordingly
		UnaryOp f = (UnaryOp) formula;
		if ( ! "sum".equals(f.getOp())) {
			throw new TodoException(f);
		}
		BasicFormula coltosum = (BasicFormula) f.right;
		RowName fcs = (RowName) coltosum.getCellSetSelector();
		String propToSum = fcs.getRowName();
		// do the sums
		for(Col col : col2items.keySet()) {
			List<Map> citems = col2items.get(col);
			// pluck and sum
			double sum = 0;
			for (Map<String, String> citem : citems) {
				String v = Containers.getLenient(citem, propToSum);
				double vn = MathUtils.getNumber(v);
				sum += vn;
			}			
			getCreateCol(col);
			Numerical n = new Numerical(sum);
			values.set(col.index, n);			
		}
	}
	
	private String run2_pickTimeColumn(List<String> headers) {
		// mapping?
		Collection<String> TIME_NAMES = Arrays.asList("month","date","time","start","start date");
		if (mappingImportRow2ourRow != null) {
			for (String h : mappingImportRow2ourRow.keySet()) {
				String mh = mappingImportRow2ourRow.get(h);				
				if (TIME_NAMES.contains(mh)) {
					return h;
				}	
			}
		}
		// look for a likely candidate
		for (String h : headers) {
			String h2 = StrUtils.toCanonical(h);
			if (TIME_NAMES.contains(h2)) {
				return h;
			}
		}
		// nope
		return null;
	}

	private Numerical getCreateCol(Col col) {
		// pad with 0s if needed
		for(int j=values.size(); j<=col.index; j++) {
			values.add(new Numerical(0));
		}
		return values.get(col.index);		
	}

	List<Numerical> values;

	@Override
	public Numerical calculate(Cell b) {
		if (values==null) {
			run(b.getBusiness());
		}
		if (b.col.index >= values.size()) {
			return null;
		}
		Numerical v = values.get(b.col.index);
		return v;
	}
}

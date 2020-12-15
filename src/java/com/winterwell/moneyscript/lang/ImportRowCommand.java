package com.winterwell.moneyscript.lang;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.winterwell.maths.datastorage.DataTable;
import com.winterwell.maths.stats.distributions.d1.Gaussian1D;
import com.winterwell.maths.stats.distributions.d1.MeanVar1D;
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

		Iterable<Map> items = (Iterable) r.asListOfMaps();
		values = new ArrayList();
		
		// Time column
		String timeCol = run2_pickTimeColumn(r.getHeaders());
		if (timeCol==null) {
			throw new IllegalArgumentException("No month/date/time column recognised in "+src);
		}
		
		// calculate what?
		// HACK assume a basic sum/count/mean, and unpack accordingly
		UnaryOp f = (UnaryOp) formula;
		String op = f.getOp();
		if ( ! "sum count mean".contains(op)) {
			throw new TodoException(f);
		}
		BasicFormula coltosum = (BasicFormula) f.right;
		RowName fcs = (RowName) coltosum.getCellSetSelector();
		String propToSum = fcs.getRowName();

		// filter out of window data
		Period period = new Period(b.getSettings().getStart(), b.getSettings().getEnd());
		List<Map> inItems = Containers.filter(items, map -> {
			Time t = getTimeForItem(timeCol, map);
			// count [start, end) so we can't double-count
			if (t.isBefore(period.first) || t.isAfterOrEqualTo(period.second)) {
				Log.d("import", "...skip (time window) "+t+" "+map);
				return false; // skip, outside time window
			}
			return true;
		});
		
		// process the items
		
		if ("aggregate".equals(slicing)) {
			Numerical n = run2_numForItems(op, propToSum, inItems);
			values.add(n);
			return;
		}
		
		// ...Split by month (column)
		ListMap<Col,Map> col2items = new ListMap<>();		
		for (Map<String, String> map : inItems) {
			String s = map.get(timeCol);
			Time t = TimeUtils.parseExperimental(s);
			Col col = b.getColForTime(t);
			col2items.add(col, map);				
		}		
		// do the sums
		values.add(null); // to make it 1-indexed
		for(Col col : col2items.keySet()) {
			List<Map> citems = col2items.get(col);
			Numerical n = run2_numForItems(op, propToSum, citems);
			getCreateCol(col);			
			values.set(col.index, n);			
		}
	}

	private Time getTimeForItem(String timeCol, Map<String, String> map) {
		String s = map.get(timeCol);
		Time t = TimeUtils.parseExperimental(s);
		return t;
	}

	private Numerical run2_numForItems(String op, String propToSum, Iterable<Map> citems) {
		// pluck and sum
		MeanVar1D mv = new MeanVar1D();
		double sum = 0;
		int cnt = 0;
		for (Map<String, ?> citem : citems) {
			String v = (String) Containers.getLenient(citem, propToSum);
			double vn = MathUtils.getNumber(v);
			sum += vn;
			mv.train1(vn);
			if ( ! Utils.isBlank(v) && ! v.equals("0") && ! v.equals("0.0")) {
				cnt++;
			}
		}			
		Numerical n;
		switch(op) {
		case "sum": 
			n = new Numerical(sum); break;
		case "count":
			n = new Numerical(cnt); break;
		case "mean":
			Gaussian1D norm = new Gaussian1D(mv.getMean(), mv.getVariance());
			n = new UncertainNumerical(norm); break;
		default:
			throw new IllegalArgumentException("TODO op "+op+" in "+this);
		}
		return n;
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
	private String slicing;

	@Override
	protected Numerical calculate2_formula(Cell b) {
		if (values==null) {
			run(b.getBusiness());
		}
		if ("aggregate".equals(slicing)) {
			return values.get(0);
		}
		if (b.col.index >= values.size()) {
			return null;
		}
		Numerical v = values.get(b.col.index);
		return v;
	}

	public void setSlicing(String slicing) {
		assert slicing.equals("by month") || slicing.equals("aggregate");
		this.slicing = slicing;
	}
}

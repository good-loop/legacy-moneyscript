package com.winterwell.moneyscript.lang;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.winterwell.maths.stats.distributions.d1.Gaussian1D;
import com.winterwell.maths.stats.distributions.d1.MeanVar1D;
import com.winterwell.moneyscript.lang.cells.RowName;
import com.winterwell.moneyscript.lang.num.BasicFormula;
import com.winterwell.moneyscript.lang.num.Numerical;
import com.winterwell.moneyscript.lang.num.UnaryOp;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Col;
import com.winterwell.utils.FailureException;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.containers.ListMap;
import com.winterwell.utils.io.CSVReader;
import com.winterwell.utils.io.CSVSpec;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Period;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;

/**
 * 
 * ImportCommand pulls in lots of rows. This only imports a single row.
 * 
 * 
 * @testedby ImportRowCommandTest
 * @author daniel
 *
 */
public class ImportRowCommand extends ImportCommand {

	private String propForComment;
	private String propToSum;

	public ImportRowCommand(String src) {
		super(src);
	}	

	@Override
	public String toString() {
		return "ImportRowCommand[src=" + src +", formula="+formula+"]";
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

		// Is this a financial columns=dates sheet?
		if ("none".equals(slicing)) {
			run2_financeSheet(b, r, headers);
			return;
		}
		// It is a SalesForce style sheet (which we analyse)
		
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
		propToSum = fcs.getRowName();
		
		propForComment = mappingImportRow2ourRow==null? null : mappingImportRow2ourRow.get("commentary");
		
		// filter out of window data
		Period period = new Period(b.getSettings().getStart(), b.getSettings().getEnd());
		List<Map> inItems = Containers.filter(items, map -> {
			Time t = getTimeForItem(timeCol, map);
			// count [start, end) so we can't double-count
			if (t.isBefore(period.first) || t.isAfterOrEqualTo(period.second)) {
				Log.d(LOGTAG, "...skip (time window) "+t+" "+map);
				return false; // skip, outside time window
			}
			return true;
		});
		
		// process the items
		
		if ("aggregate".equals(slicing)) {
			Numerical n = run2_numForItems(op, inItems);
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
			Numerical n = run2_numForItems(op, citems);
			// If we have no data for a column -- 0 sales! Don't let the predictive formulae fill in a number later on
			Numerical zero = new Numerical(0);
			zero.comment = IMPORT_MARKER_COMMENT;
			getCreateCol(col, zero);	
			values.set(col.index, n);			
		}
	}

	private void run2_financeSheet(Business b, CSVReader r, List<String> headers) {

		String importRowName = ((BasicFormula)formula).getCellSetSelector().getSrc();

		ourCol4importCol = run2_importRows2_ourCol4importCol(b, headers.toArray(new String[0]), b.getSettings().getStart(), b.getSettings().getEnd());
		// check we found something
		int colsFound = 0;
		for (Col col : ourCol4importCol) {
			if (col!=null) colsFound++;
		}		
		if (colsFound==0) {
			throw new FailureException(
					"No columns identified from "+StrUtils.join(headers, ", ")
					+" Errors: "+importCol_exs+" Outside Time Window: "+importCol_outsideTimeWindow);
		}		
		// find our row
		for (String[] row : r) {
			if (row.length==0) continue;
			String rName = row[0];
			if ( ! run3_financeSheet2_matchNames(importRowName, rName)) {
				continue;
			}
			String[] theRow = row;
			values = new ArrayList();
			values.add(null); // 1 indexed
			// manage time alignment
			// NB: copy pasta from ImportCommand
			// add in the data
			for (int i = 1; i < row.length; i++) {
				if (i >= ourCol4importCol.length) {
					Log.e(LOGTAG, "Overlong row? "+i+" from "+importRowName);
					break;
				}
				Col col = ourCol4importCol[i];				
				if (col==null) {
//					vs.add(null);
					continue; // skip e.g. not in the sheet's time window
				}
				String ri = row[i];
				double n = MathUtils.getNumber(ri);
				if (n == 0 && (ri==null || ! "0".equals(ri.trim()))) {
//					vs.add(null);
					continue; // skip blanks and non-numbers but not "true" 0s
				}
				Numerical v = new Numerical(n);
				v.comment = IMPORT_MARKER_COMMENT;				
				getCreateCol(col, null);
				values.set(col.index, v);
			}

			return;	
		}
		throw new FailureException("Row "+importRowName+" not found");
	}

	private boolean run3_financeSheet2_matchNames(String importRowName, String rName) {
		return StrUtils.toCanonical(importRowName).equals(StrUtils.toCanonical(rName));
	}

	private Time getTimeForItem(String timeCol, Map<String, String> map) {
		String s = map.get(timeCol);
		Time t = TimeUtils.parseExperimental(s);
		return t;
	}

	private Numerical run2_numForItems(String op, Iterable<Map> citems) {
		// pluck and sum
		MeanVar1D mv = new MeanVar1D();
		double sum = 0;
		int cnt = 0;		
		StringBuilder comment = propForComment==null? null : new StringBuilder();
		for (Map<String, ?> citem : citems) {
			String v = (String) Containers.getLenient(citem, propToSum);
			double vn = MathUtils.getNumber(v);
			sum += vn;
			mv.train1(vn);
			if ( ! Utils.isBlank(v) && ! v.equals("0") && ! v.equals("0.0")) {
				cnt++;
			}
			if (comment!=null) {
				String c = (String) Containers.getLenient(citem, propForComment);
				if (c !=null) { comment.append(c); comment.append(", "); }
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
		if (comment!=null) {
			n.comment = comment.toString();
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

	/**
	 * @deprecated Not needed here - the row is given by the rule
	 */
	@Override
	public void run2_importRows(Business b) {
		Log.d(LOGTAG, "run2_importRows() no-op for ImportRowCommand");
	}
	
	/**
	 * 
	 * @param col
	 * @param padding Pad with nulls (if you want M$ calculations to fill-in) or 0s (if you want 0s)
	 * @return
	 */
	private Numerical getCreateCol(Col col, Numerical padding) {
		// pad with nulls if needed
		for(int j=values.size(); j<=col.index; j++) {
			values.add(padding);
		}
		return values.get(col.index);		
	}

	/**
	 * 1 Indexed!
	 */
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
		slicing = slicing.trim();
		assert "by month aggregate none".contains(slicing);
		this.slicing = slicing;
	}
}

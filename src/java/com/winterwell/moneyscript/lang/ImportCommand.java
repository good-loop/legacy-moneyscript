package com.winterwell.moneyscript.lang;

import java.io.File;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.tree.RowMapper;

import com.winterwell.moneyscript.data.PlanDoc;
import com.winterwell.moneyscript.lang.num.Numerical;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Col;
import com.winterwell.moneyscript.output.Row;
import com.winterwell.nlp.dict.Dictionary;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.ArraySet;
import com.winterwell.utils.containers.Cache;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.CSVReader;
import com.winterwell.utils.io.CSVSpec;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;
import com.winterwell.utils.web.IHasJson;
import com.winterwell.utils.web.WebUtils;
import com.winterwell.web.FakeBrowser;
import com.winterwell.web.ajax.JSend;
import com.winterwell.web.ajax.JThing;

/**
 * e.g. import some actuals
 * import: https://docs.google.com/spreadsheets/meh?output=csv {name:"actuals", rows:"overlap"}
 *
 * e.g. TODO import SF deal data
 *  
 * @author daniel
 * @testedby {@link ImportCommandTest}
 */
public class ImportCommand extends Rule implements IHasJson {

	@Override
	protected Numerical calculate2_formula(Cell b) {
		return null;
	}

	/**
	 * magic-string value for rows = "import the ones which overlap with our rules"
	 */
	private static final String OVERLAP = "overlap";

	public ImportCommand(String src) {
		super(null, null, src, 0);
		// HACK g-drive sources
		if (src.startsWith("https://docs.google.com/spreadsheets/")) {
			if (src.contains("gviz")) {
				// remove the gviz bit to get a normal url
				url = src.substring(0, src.indexOf("/gviz"));
			} else {	
				// convert a normal url to a gviz
				Pattern gsu = Pattern.compile("https://docs.google.com/spreadsheets/d/([^/]+)");
				Matcher m = gsu.matcher(src);
				if (m.matches()) {
					String docid = m.group(1);
					url = src;
					src = url.substring(0, m.end())+"/gviz/tq?tqx=out:csv";					
				}
			}
		}
	}	
	
	private List<String> rows = Arrays.asList(OVERLAP);
	
	/**
	 * TODO
	 */
	protected boolean overwrite = true;

	@Override
	public String toString() {
		return "ImportCommand[src=" + src + "]";
	}
	
	Dt cacheDt = new Dt(10, TUnit.MINUTE); 
	
	transient String csv;
	
	static Cache<String, String> csvCache = new Cache<>(20);
	
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
		Dictionary rowNames = b.getRowNames(); // do this now, so we can support fuzzy matching but not give a fuzzy
												// match against the csv's own rows
		List<String> headers = r.getHeaders();
		// match headers to columns
		String h1 = headers.get(1);
		int col1;
		// is it a time?
		try {
			Time time = TimeUtils.parseExperimental(h1);
			// HACK
			if (time.isBefore(b.getSettings().getStart())) {
				Dt dt = time.dt(b.getSettings().getStart());
				double i = dt.divide(b.getSettings().timeStep);
				col1 = (int) Math.round(1 - i); // i steps back from 1st col
			} else if (time.isAfter(b.getSettings().getStart())) {
				Log.w("Business.import", "Skip import " + h1);
				return; // import nothing
			} else {
				col1 = b.getColForTime(time).index;
			}
		} catch (Exception ex) {
			// Not a time 
			Log.d("Business.import", "(oh well) First row of csv import does not seem to be a list of times: \""+h1+ "\" > " + ex);
			col1 = 1;
		}

		for (String[] row : r) {
			if (row.length == 0)
				continue;
			String rowName = row[0];
			if (Utils.isBlank(rowName))
				continue;
			// match row name
			String ourRowName = run2_ourRowName(rowName, rowNames);
			if (ourRowName==null) ourRowName = StrUtils.toTitleCase(rowName);
			// get/make the row
			Row brow = b.getRow(ourRowName);
			if (brow == null) {
				if (isEmptyRow(row)) {
					continue;
				}
				if (rows.contains(OVERLAP)) {
					Log.d("import", "Skip non-overlap row "+rowName);
					continue; // don't import this row
				}
				brow = new Row(ourRowName);
				b.addRow(brow);
			}
			// add in the data
			for (int i = 1; i < row.length; i++) {
				String ri = row[i];
				double n = MathUtils.getNumber(ri);
				if (n == 0 && (ri==null || ! "0".equals(ri.trim()))) {
					continue; // skip blanks and non-numbers but not "true" 0s
				}
				int j = col1 + i - 1;
				if (j < 1) {
					// skip until we enter the sheet's time window
					continue;
				}
				Col col = new Col(j);
				Cell cell = new Cell(brow, col);
				Numerical v = new Numerical(n);
				v.comment = "import";
				// Set value
				b.state.set(cell, v);
			}
		}
		// error level?
//		r.getB
	}


	transient Time fetched;

	String name;

	String url;

	/**
	 * map incoming names to our names
	 */
	protected Map<String, String> mappingImportRow2ourRow;
	
	protected void fetch() {
		// Always use in memory if set (e.g. if doing samples: 20))
		if (csv !=null) {
			return;
		}
		// cached?
		String cached = csvCache.get(src);
		if ( ! Utils.isBlank(cached) && cacheDt!=null && fetched != null
				&& fetched.plus(cacheDt).isAfter(new Time())) 
		{
			// use cache
			csv = cached;
			Log.d("import", "use cached "+src);
			return;
		}
		fetched = new Time();
		// is it a file?
		if (src.startsWith("file:")) {
			URI u = WebUtils.URI(src);
			String fpath = u.getPath();
			csv = FileUtils.read(new File(fpath));
		} else { // fetch (TODO with some cache)
			FakeBrowser fb = new FakeBrowser();
			fb.setFollowRedirects(true);
			csv = fb.getPage(src);
		}
		// HACK Is it a JSend wrapper?
		if (src.endsWith(".ms") || src.endsWith(".m$") || src.endsWith(".json")) {
			JSend jsend = JSend.parse(csv);
			JThing<PlanDoc> data = jsend.getData().setType(PlanDoc.class);
			PlanDoc pd = data.java();
			csv = pd.getText();
			name = Utils.or(pd.getName(), pd.getId());
		}		
		// cache
		if (cacheDt!=null && cacheDt.getValue() > 0) {
			csvCache.put(src, csv);
		}
		Log.d("import", "fetched "+src);
	}

	/**
	 * 
	 * @param row
	 * @return false if any entries are non-zero
	 */
	private boolean isEmptyRow(String[] row) {
		// is it empty?
		for (int i = 1; i < row.length; i++) {
			String ri = row[i];
			double n = MathUtils.getNumber(ri);
			if (n != 0) {
				return false;
			}
		}
		return true;
	}

	private String run2_ourRowName(String rowName, Dictionary rowNames) {
		// mapping?
		if (mappingImportRow2ourRow != null) {
			String mappedName = Containers.getLenient(mappingImportRow2ourRow, rowName);
			if (mappedName!=null) {
				// TODO try correcting for slight mismatches?? e.g. using this method recursively??
				return mappedName;
			}
		}
		// exact match
		if (rowNames.contains(rowName)) {
			return rowNames.getMeaning(rowName);
		}
		// match ignoring case+
		String rowNameCanon = StrUtils.toCanonical(rowName);
		if (rowNames.contains(rowNameCanon)) {
			return rowNames.getMeaning(rowNameCanon);
		}
		// match on ascii
		String rowNameAscii = rowNameCanon.replaceAll("[^a-zA-Z0-9]", "");
		if ( ! rowNameAscii.isEmpty() && rowNames.contains(rowNameAscii)) {
			return rowNames.getMeaning(rowNameAscii);
		}
		// try removing "total" since MS group rows are totals
		if (rowNameCanon.contains("total")) {			
			String rn2 = rowNameCanon.replace("total", "");
			assert rn2.length() < rowNameCanon.length();
			if ( ! rn2.isBlank()) {
				String found = run2_ourRowName(rn2, rowNames);
				if (found!=null) {
					return found;
				}
			}
		}
		// Allow a first-word or starts-with match if it is unambiguous e.g. Alice = Alice Smith
		ArraySet<String> matches = new ArraySet();
		String firstWord = rowNameCanon.split(" ")[0];
		for(String existingName : rowNames) {
			String existingNameFW = existingName.split(" ")[0];
			if (firstWord.equals(existingNameFW)) {
				matches.add(rowNames.getMeaning(existingName));
			}
		}
		if (matches.size() == 1) {
			return matches.first();
		}
		if (matches.size() > 1) {
			Log.d("import", "(skip match) Ambiguous 1st word matches for "+rowName+" to "+matches);
		}
		// starts-with?
		matches.clear();
		for(String existingName : rowNames) {
			if (rowName.startsWith(existingName)) {
				matches.add(rowNames.getMeaning(existingName));
			} else if (existingName.startsWith(rowName)) {
				matches.add(rowNames.getMeaning(existingName));
			}
		}
		if (matches.size() == 1) {
			return matches.first();
		}
		if (matches.size() > 1) {
			Log.d("import", "(skip match) Ambiguous startsWith matches for "+rowName+" to "+matches);
		}
		// Nothing left but "Nope"
		return null;
	}

	@Override
	public Map toJson2() throws UnsupportedOperationException {
		return new ArrayMap(
			"src", src,
			"name", name,
			"url", url
		);
	}

	public Business runImportMS(Lang lang) {
		fetch();		
		Business b2 = lang.parse(csv);
		return b2;
	}

	public void setRows(List<String> rows) {
		this.rows = rows;
	}
	
	/**
	 * 
	 * @param rows e.g. "overlap"
	 */
	public void setRows(String row) {
		setRows(Arrays.asList(row));
	}

	public void setMapping(Map<String,String> mappingImportRow2ourRow) {
		this.mappingImportRow2ourRow = mappingImportRow2ourRow;
	}
}

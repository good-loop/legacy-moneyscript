package com.winterwell.moneyscript.lang;

import java.io.File;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.goodloop.gsheets.GSheetsClient;
import com.winterwell.es.ESNoIndex;
import com.winterwell.moneyscript.data.PlanDoc;
import com.winterwell.moneyscript.data.PlanSheet;
import com.winterwell.moneyscript.lang.num.Numerical;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Col;
import com.winterwell.moneyscript.output.Row;
import com.winterwell.nlp.dict.Dictionary;
import com.winterwell.nlp.dict.NameMapper;
import com.winterwell.utils.FailureException;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Cache;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.containers.Trio;
import com.winterwell.utils.io.CSVReader;
import com.winterwell.utils.io.CSVSpec;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeParser;
import com.winterwell.utils.web.IHasJson;
import com.winterwell.utils.web.WebUtils;
import com.winterwell.web.FakeBrowser;
import com.winterwell.web.WebEx;
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
public class ImportCommand extends Rule implements IHasJson, IReset {

	@Override
	public void reset() {
		super.reset();
		csv = null;
		error = null;
	}
	
	/**
	 * If set, there IS a row that provides column times.
	 * -- and columns which can't beparsed should be skipped.
	 *  
	 * 1-indexed to match spreadsheets
	 */
	@ESNoIndex
	Integer timeRow;

	/**
	 * 1-indexed to match spreadsheets
	 * @param timeRow
	 */
	public void setTimeRow(int timeRow) {
		this.timeRow = timeRow;
	}
	
	@Override
	protected Numerical calculate2_formula(Cell b) {
		return null;
	}

	/**
	 * magic-string value for rows = "import the ones which overlap with our rules".
	 * This _is_ set by default. Removing it switches to "all"
	 */
	public static final String OVERLAP = "overlap";

	public static final String ALL_ROWS = "all";


	protected static final String LOGTAG = "import";
	public static final String IMPORT_MARKER_COMMENT = "import";

	/**
	 * @param src spreadsheet url 
	 */
	public ImportCommand(String src) {
		super(null, null, src, 0);
		this.url = src;
		// HACK g-drive sources ??move to GSSHeetsClient
		if ( ! src.startsWith("https://docs.google.com/spreadsheets/")) return;
		if (src.contains("gviz")) {
			// remove the gviz bit to get a normal url
			url = src.substring(0, src.indexOf("/gviz"));
		}
	}	
	
	@ESNoIndex
	protected List<String> rows = Arrays.asList(OVERLAP);
	
	/**
	 */
	@ESNoIndex
	protected boolean overwrite = true;

	@Override
	public String toString() {
		return getClass().getSimpleName()+"[src=" + src + "]";
	}
	
	private Dt cacheDt = new Dt(20, TUnit.MINUTE);
	
	public void setCacheDt(Dt cacheDt) {
		this.cacheDt = cacheDt;
	}
	
	transient String csv;

	protected Throwable error;

	protected transient Col[] ourCol4importCol;

	protected int importCol_exs;

	private int importCol_ok; // ??

	protected int importCol_outsideTimeWindow;
	
	/**
	 * url to csv,name,fetch-time
	 * 
	 * ??why not use a guava cache with built-in time handling??
	 */
	static Map<String, Trio<String,String,Time>> csvCache = new Cache<>(20);
	
	private List<String> exempt = new ArrayList<String>();
	
	transient NameMapper nameMapper;

	private PlanSheet planSheet; // TODO
	
	/**
	 * NB: Run before run(), as the row names are needed earlier to setup the BusinessState
	 * 
	 * This should be idempotent
	 * @param b
	 */
	public void run2_importRows(Business b) {		
		// Is it another m$ file??
		if (src.endsWith(".m$") || src.endsWith(".ms")) {			
			return; // Should be done already during parse!
		}
		// fetch
		fetch();
		// not a csv?
		if (csv.startsWith("<!doctype ") || csv.startsWith("<html")
			|| csv.startsWith("<!DOCTYPE ") || csv.startsWith("<HTML")) {
			throw new IllegalArgumentException("Import fail: Url "+src+" returned a web page NOT a csv. Check the url is publicly shared.");
		}
		// CSV
		CSVSpec spec = new CSVSpec();
		CSVReader r = new CSVReader(new StringReader(csv), spec);

		// headers
		int hrow = timeRow!=null? timeRow : 1; // 1-indexed
		for(int i=1; i<hrow; i++) {
			r.next();
		}
		String[] headers = r.next();
		r.setHeaders(headers);	
		r.setNumFields(-1); // flex
		
		Dictionary rowNames = b.getRowNames(); // do this now, so we can support fuzzy matching but not give a fuzzy
												// match against the csv's own rows		
		nameMapper = new NameMapper(rowNames); 
		for (String[] row : r) {
			if (row.length == 0)
				continue;
			String rowName = row[0];
			if (Utils.isBlank(rowName))
				continue;
			
			// if a row is empty, it is a header row and can be ignored safely
			if (isEmptyRow(row))
				continue;
			
			// match row name
			String ourRowName = nameMapper.run2_ourRowName(rowName);
			if (ourRowName==null) {
				ourRowName = StrUtils.toTitleCase(rowName);
				Log.d(LOGTAG, "Unmapped row: "+rowName);
				nameMapper.run2_ourRowName(rowName); // for debug
			} 
			
			nameMapper.putTheirsOurs(rowName, ourRowName);
			
			// get/make the row
			Row brow = b.getRow(ourRowName);
			if (brow == null) {
				if (isEmptyRow(row)) {
					continue;
				}
				if (isOverlap()) {
//					Log.d(LOGTAG, "Skip non-overlap row "+rowName);
					continue; // don't import this row
				}
				brow = new Row(ourRowName);
				b.addRow(brow, this.planSheet); // TODO 
			}
		}	
		if (mappingImportRow2ourRow==null) mappingImportRow2ourRow = new HashMap();
		mappingImportRow2ourRow.putAll(nameMapper.getOurNames4TheirNames());
		exempt = nameMapper.getTheirAmbiguous();
		
		// match headers to columns
		// NB: 1-indexed, so [0] = null
		ourCol4importCol = run2_importRows2_ourCol4importCol(b, headers, b.getSettings().getStart(), b.getSettings().getEnd());
		// check we found something
		int colsFound = 0;
		for (Col col : ourCol4importCol) {
			if (col!=null) colsFound++;
		}		
		if (colsFound==0) {
			setError(new FailureException(
					"No columns identified! from "+StrUtils.join(headers, ", ")
					+" Errors: "+importCol_exs+" Outside Time Window: "+importCol_outsideTimeWindow
					));
		}
	}
	

	public boolean isOverlap() {
		return rows.contains(OVERLAP);
	}
	
	public void run(Business b) {
		try {
			// Is it another m$ file??
			if (src.endsWith(".m$") || src.endsWith(".ms")) {			
				return; // Should be done already during parse!
			}
			fetch();
			// import rows and setup b-state
			run2_importRows(b);
			if (mappingImportRow2ourRow.isEmpty()) {
				throw new IllegalStateException("No rows to import from "+src+" with rows: "+rows);
			}
			// CSV			
			CSVSpec spec = new CSVSpec();
			CSVReader r = new CSVReader(new StringReader(csv), spec);
	
			// headers
			int hrow = timeRow!=null? timeRow : 1; // 1-indexed
			for(int i=1; i<hrow; i++) {
				r.next();
			}
			String[] headers = r.next();
			r.setHeaders(headers);	
			r.setNumFields(-1); // flex
			
			// cols -- done already
			
			for (String[] row : r) {
				run2_row(row, b);
			}
			// all good
			error = null;
		} catch (Throwable ex) {
			error = ex;
			throw Utils.runtime(ex);
		}
	}
	
	/** for Import, we want to skip blanks. But for Compare we want them. */
	boolean blankIsZero = false;			

	void run2_row(String[] row, Business b) {
		if (isEmptyRow(row)) {
			return;
		}
		String srcRowName = row[0];
		if (Utils.isBlank(srcRowName)) {
			return;
		}
		
		// Hack to prevent null pointer exception for ambiguous rows
		if (exempt.contains(srcRowName)) {
			return;
		}
		
		// match row name
		String ourRowName = mappingImportRow2ourRow.get(srcRowName);
		assert ourRowName != null : srcRowName;
		// get/make the row
		Row brow = b.getRow(ourRowName);
		if (brow == null) {
			if (isEmptyRow(row)) {
				return;
			}
			if (isOverlap()) {
//				Log.d(LOGTAG, "Skip non-overlap row "+rowName);
				return; // don't import this row
			}
			assert false;
		}
				
		// add in the data
		for (int i = 1; i < row.length; i++) {
			if (i >= ourCol4importCol.length) {
				Log.e(LOGTAG, "Overlong row? "+i+" from "+srcRowName);
				break;
			}
			Col col = ourCol4importCol[i];
			if (col==null) {
				continue; // skip e.g. not in the sheet's time window
			}
			String ri = row[i];
			double n = MathUtils.getNumber(ri);
			if (
				n == 0 && ! blankIsZero && ! (ri!=null && "0".equals(ri.trim())) 
			) {
				continue; // skip blanks and non-numbers but not "true" 0s
			}
			Cell cell = new Cell(brow, col);
			Numerical v = run2_setCellValue(b, n, cell, srcRowName);
			// tag
			// ??Should this be later where it can follow selector logic in tagging rules?
			brow.tagImport(cell, v);
		}		
	}

	protected Col[] run2_importRows2_ourCol4importCol(Business b, String[] headers, Time start, Time end) {
		Col[] _ourCol4importCol = new Col[headers.length];
		TimeParser tp = new TimeParser();
		importCol_exs=0; 
		importCol_ok=0; 
		importCol_outsideTimeWindow=0;
		for(int i=0; i<headers.length; i++) {
			try {
				String hi = headers[i].trim(); // 0=row labels	
				if (Utils.isBlank(hi)) continue;
				Time time = tp.parseExperimental(hi);
				// NB: don't include plain years, e.g. "2020", which are probably annual sums
				if (hi.matches("\\d{4}")) {
					continue;
				}
				importCol_ok++;
				if (time.isBefore(start)) {
					importCol_outsideTimeWindow++;
					continue; // leave null
				} else if (time.isAfter(end)) {
					importCol_outsideTimeWindow++;
					continue; // leave null
				} 
				Col coli = b.getColForTime(time);
				if (i>0) {
					Col prev = _ourCol4importCol[i-1];
					if (prev!=null) {
						int dt = coli.index - prev.index;
						if (dt != 1) {									
							Log.w(LOGTAG, "WRONG col alignment probably d: "+dt);
							Col coli2 = b.getColForTime(time);
						}
					}
				}
				_ourCol4importCol[i] = coli;
			} catch(Exception ex) {
				importCol_exs++;
				// leave null
			}
		}
		return _ourCol4importCol;
	}

	Numerical run2_setCellValue(Business b, double n, Cell cell, String srcRowName) {
		Numerical v = new Numerical(n);
		v.comment = IMPORT_MARKER_COMMENT+" "+srcRowName+" from "+Utils.or(name, url);
		// Set value
		b.state.set(cell, v);
		return v;
	}

	String name;

	/**
	 * The url if someone wishes to visit the spreadsheet
	 */
	String url;

	/**
	 * map incoming names to our names -- this can be set by the user in the M$ script
	 */
	@ESNoIndex
	protected Map<String, String> mappingImportRow2ourRow;

	private String varName;
	
	public void fetch() {
		// Always use in memory if set (e.g. if doing samples: 20))
		if (csv !=null) {
			if (csv.startsWith("ERROR:")) {	// HACK - not used yet
				throw new WebEx.E400(csv.substring(6));
			}
			return;
		}
		// cached?
		String csvUrl = getCsvUrl();
		Trio<String, String, Time> cached = csvCache.get(csvUrl);
		if (cached != null && cacheDt!=null 
				&& cached.third.plus(cacheDt).isAfter(new Time())) 
		{
			// use cache
			csv = cached.first;
			if (name==null) name = cached.second;
			Log.d(LOGTAG, "use cached "+csvUrl);
			return;
		}
		Time fetched = new Time();
		Log.d(LOGTAG, "fetch "+csvUrl+"...");
		try {
			// is it a file?
			if (csvUrl.startsWith("file:")) {
				URI u = WebUtils.URI(csvUrl);
				String fpath = u.getPath();
				csv = FileUtils.read(new File(fpath));
			} else { // fetch
				FakeBrowser fb = new FakeBrowser();
				fb.setFollowRedirects(true);
				csv = fb.getPage(csvUrl);
				// NB: no name info provided 
			}
			// HACK Is it a JSend wrapper?
			if (csvUrl.endsWith(".ms") || csvUrl.endsWith(".m$") || csvUrl.endsWith(".json")) {
				JSend jsend = JSend.parse(csv);
				JThing<PlanDoc> data = jsend.getData().setType(PlanDoc.class);
				PlanDoc pd = data.java();
				csv = pd.getText();
				name = Utils.or(pd.getName(), pd.getId());
			}	
		} catch(Exception ex) {
			// ??cache the error as "ERROR:"+message No - allow the user to make quick edits if theres an error.
			setError(ex);
			throw Utils.runtime(ex);
		}
		// cache		
		if (cacheDt!=null && cacheDt.getValue() > 0) {
			csvCache.put(csvUrl, new Trio(csv, name, fetched));
		}
		Log.d(LOGTAG, "fetched "+csvUrl);
	}

	private String getCsvUrl() {
		if (src.contains("gviz") || src.contains(".csv")) {
			return src;
		}
		// convert a normal g-sheet url to a gviz
		String docId = GSheetsClient.getSpreadsheetId(src);
		if (docId==null) {
			return src;
		}
		String csrc = "https://docs.google.com/spreadsheets/d/"+docId+"/gviz/tq?tqx=out:csv";
		// gid? NB: G-sheets doesn't use proper url parameters
		Pattern GID = Pattern.compile("gid=(\\d+)");
		Matcher m = GID.matcher(src);
		if (m.find()) {
			csrc += "&gid="+m.group(1);
		}
		return csrc;
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

	@Override
	public Map toJson2() throws UnsupportedOperationException {
		return new ArrayMap(
			"src", src,
			"rows", rows,
			"scenario", getScenario(),
			"name", name,
			"url", url,
			"error", error
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
		setRows(Arrays.asList(row.trim()));
	}

	/**
	 * You can specify this in the script, by {sheet row: our row} in the json-like settings
	 * @param mappingImportRow2ourRow
	 */
	public void setMapping(Map<String,String> mappingImportRow2ourRow) {
		this.mappingImportRow2ourRow = mappingImportRow2ourRow;
	}

	public Throwable getError() {
		return error;
	}

	public void setError(Exception ex) {
		this.error = ex;
	}

	public static boolean isImported(Numerical v) {
		return v.comment != null && v.comment.startsWith(IMPORT_MARKER_COMMENT);
	}

	/**
	 * Set only for "import as Foo: csv" commands
	 * @return
	 */
	public String getVarName() {
		return varName;
	}

	public void setVarName(String x) {
		this.varName = x;
	}

	public void clearCache() {
		String cu = getCsvUrl();
		csvCache.remove(cu);
	}
	
}
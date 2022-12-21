package com.winterwell.moneyscript.lang;

import static com.winterwell.nlp.simpleparser.Parsers.chain;
import static com.winterwell.nlp.simpleparser.Parsers.first;
import static com.winterwell.nlp.simpleparser.Parsers.ignore;
import static com.winterwell.nlp.simpleparser.Parsers.lit;
import static com.winterwell.nlp.simpleparser.Parsers.num;
import static com.winterwell.nlp.simpleparser.Parsers.opt;
import static com.winterwell.nlp.simpleparser.Parsers.optSpace;
import static com.winterwell.nlp.simpleparser.Parsers.regex;
import static com.winterwell.nlp.simpleparser.Parsers.seq;
import static com.winterwell.nlp.simpleparser.Parsers.space;
import static com.winterwell.nlp.simpleparser.Parsers.word;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;

import com.winterwell.moneyscript.lang.cells.CellSet;
import com.winterwell.moneyscript.lang.cells.LangCellSet;
import com.winterwell.moneyscript.lang.cells.RowName;
import com.winterwell.moneyscript.lang.cells.Scenario;
import com.winterwell.moneyscript.lang.num.BasicFormula;
import com.winterwell.moneyscript.lang.num.Formula;
import com.winterwell.moneyscript.lang.num.LangNum;
import com.winterwell.moneyscript.lang.time.DtDesc;
import com.winterwell.moneyscript.lang.time.LangTime;
import com.winterwell.moneyscript.lang.time.SpecificTimeDesc;
import com.winterwell.moneyscript.lang.time.TimeDesc;
import com.winterwell.nlp.simpleparser.AST;
import com.winterwell.nlp.simpleparser.PP;
import com.winterwell.nlp.simpleparser.ParseFail;
import com.winterwell.nlp.simpleparser.ParseResult;
import com.winterwell.nlp.simpleparser.Parser;
import com.winterwell.nlp.simpleparser.Parsers;
import com.winterwell.nlp.simpleparser.Ref;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;
import com.winterwell.utils.web.WebUtils;
import static com.winterwell.nlp.simpleparser.Parsers.ref;

/**
 * Style (css), charts, etc
 * 
 * e.g. `columns: 1 year` 
 * {@link LangMiscTest}
 */ 
public class LangMisc {
	

	static Parser<String> meta = lit("hide", "show", "plot distribution", "plot", "off").label("meta");

	static Parser<String> css = new PP<String>(regex("\\{([^\\}]*)\\}")) {
		@Override
		protected String process(ParseResult<?> r) throws ParseFail {
			MatchResult m = (MatchResult) r.getX();
			return (String) m.group(1);
		}		
	}.label("css").eg("{color:red;}"
			+ "").setCanBeZeroLength(false);
	
	public static final Parser comment = regex("\\s*//.*").label("comment").setCanBeZeroLength(false);	

	/**
	 * always matches and returns 0+
	 */
	static Parser<Number> indent = new PP<Number>(opt(space)){
		protected Number process(ParseResult<?> r) {
			return r.ast.getValue().length();
		};
	}.label("indent");

	protected static Parser<MatchResult> urlOrFile = Parsers.regex(WebUtils.URL_REGEX.pattern()).label("url");
	

	private Parser<Settings> periodSetting = new PP<Settings>(
//			LangTime.time TODO
			seq(LangTime.dt, opt(seq(space, word("from"), space, LangTime.time))) 
	) {
		protected Settings process(ParseResult<?> r) {
			Settings s = new Settings();
//			TimeDesc td = (TimeDesc) r.getX(); TODO
			AST<DtDesc> ndt = r.getNode(LangTime.dt);
			s._runTime =  ndt.getX().calculate(null);
			return s;
		}
	}.eg("1 year from Jan 2020");

	private Parser<Settings> cssSetting = new PP<Settings>(css) {
		protected Settings process(ParseResult<?> r) throws ParseFail {
			String p = r.parsed();
			Settings s = new Settings();
			s.css = p;
			return s;
		}
	};
	
	private Parser<Settings> samplesSetting = new PP<Settings>(
			seq(ignore("samples:"), optSpace, num("n"))) {
		@Override
		protected Settings process(ParseResult<?> r) throws ParseFail {
			Settings s = new Settings();
			Double n = (Double) r.getLeafValues().get(0);
			if (n != Math.round(n)) {
				throw new ParseFail(r, "Number of samples must be a round number");
			}
			s.samples = n.intValue();
			return s;
		}
	};
	
	private Parser<Settings> columnSettingsMeat = (Parser) first(
			periodSetting, cssSetting
	).onFail("Not a valid column setting");
	
	private PP<Settings> columnMultiSettings = new PP<Settings>(
			chain(columnSettingsMeat, regex(",\\s+"))
	) {
		protected Settings process(ParseResult<?> r) {
			List<Settings> ls = r.getLeafValues();
			if (ls.size()==1) return ls.get(0);
			Settings s = new Settings();
			for (Object s2 : ls) {
				s.merge((Settings) s2);
			}
			return s;
		}
	}.label("columnMultiSettings");
	
	/**
	 * use-case??
	 */
	Parser<Settings> columnSettings = seq(
				lit("columns").label(null), lit(":").label(null), optSpace, 
				opt(columnMultiSettings), opt(comment), optSpace);

	
	PP<Settings> startEndSetting = new PP<Settings>(
			seq(lit("start", "end", "year end", "currency"), lit(":"), optSpace, LangTime.time)
			) {
		protected Settings process(ParseResult<?> r) {
			Settings s = new Settings();
			String keyword = (String) r.getLeafValues().get(0);
			AST<TimeDesc> ptime = r.getNode(LangTime.time);
			TimeDesc timeDesc = ptime.getX();
			Time time = timeDesc.getTime();
			if ("start".equals(keyword)) {
				s.setStart(time);
			}
			if ("end".equals(keyword) && timeDesc instanceof SpecificTimeDesc) {				
				// HACK end of month?				
				String tdesc = timeDesc.getDesc(); // TODO what if they ask for a specific day?
				Time eom = TimeUtils.getEndOfMonth(time);
				eom = eom.minus(TUnit.MILLISECOND); // in the month, just
				s.setEnd(eom);
			}
			if ("year end".equals(keyword) && timeDesc instanceof SpecificTimeDesc) {				
				s.setYearEnd(time.getMonth());
			}
			return s;
		}
	};
	
	/**
	 * TODO flexible cache settings for imports.
	 * fresh = no cache
	 */
	Parser<String> cache = lit(" fresh");

	/**
	 * key:value - doesn't require ""s
	 */
	Parser<Map> jsonLike1KeyVal = new PP<Map>(regex("\"?[a-zA-Z0-9 \\-_]+\"?:\\s*[^},]+")) {
		@Override
		protected Map process(ParseResult<?> r) throws ParseFail {
			String kv = r.parsed();
			int i = kv.indexOf(':');
			String k = unquote(kv.substring(0, i).trim());
			String v = unquote(kv.substring(i+1).trim());
			return new ArrayMap(k,v);
		}		
	}.label("jsonLike1KeyVal");
	
	@Deprecated // temp copy-pasta from StrUtils to keep the server compiler happy until open-code updates 
	public static String unquote(String s) {
		if (s==null) return null;
		if (s.startsWith("\"") && s.endsWith("\"")) {
			s = s.substring(1, s.length()-1);
		}
		if (s.startsWith("'") && s.endsWith("'")) {
			s = s.substring(1, s.length()-1);
		}
		return s;
	}
	
	Parser<Map> jsonLike = new PP<Map>(
			seq(lit("{"), chain(jsonLike1KeyVal, seq(optSpace,lit(","),optSpace)), lit("}"))
	) {
		@Override
		protected Map process(ParseResult<?> pr) throws ParseFail {
			List<AST> leaves = (List) pr.ast.getLeaves();
			Map jobj = new ArrayMap();
			for (AST ast : leaves) {
				Object kv = ast.getX();
				// NB: skip spacing
				if (kv instanceof Map) {
					jobj.putAll((Map)kv); 
				}
			}
			return jobj;
		}		
	}.label("jsonLike");
	
	/**
	 * e.g. import actuals from a csv. See also importRule
	 * 
	 * TODO other blend modes?
	 */
	PP<ImportCommand> importCommand = new PP<ImportCommand>(
			seq(lit("import", "compare"), opt(cache), opt(lit(" all"," overlap").label("whichRows")),
					opt(seq(lit(" as "), ref(LangCellSet.ROW_NAME))),
					lit(":"), 
					optSpace, LangMisc.urlOrFile, 
					optSpace, opt(jsonLike))
			) {
		protected ImportCommand process(ParseResult<?> r) {						
			AST<MatchResult> psrc = r.getNode(LangMisc.urlOrFile);
			// import or compare?
			ImportCommand s = new ImportCommand(psrc.parsed());
			String parsed = r.parsed();
			if (parsed.startsWith("compare")) {
				s = new CompareCommand(psrc.parsed());
			}
			s.overwrite = true;			
			// cache settings?
			AST<String> isFresh = r.getNode(cache);
			if (isFresh != null) {
				s.setCacheDt(TimeUtils.NO_TIME_AT_ALL); 
			}
			// rows: all / overlap? (can also be set in the json key:value)
			AST<String> whichRows = r.getNode("whichRows");
			if (whichRows != null) {
				s.setRows(whichRows.getX()); 
			}			
			AST<String> rName = r.getNode(LangCellSet.ROW_NAME);
			if (rName != null) {
				s.setVarName(rName.getValue().toString());
			}
			// extra info
			AST<Map> _jobj = r.getNode(jsonLike);
			if (_jobj!=null) {
				Map jobj = _jobj.getX();
				if (jobj.containsKey("name")) {
					s.name = (String) jobj.get("name");
				}
				if (jobj.containsKey("url")) {
					s.url = (String) jobj.get("url");
				}
				if (jobj.containsKey("rows")) {
					// e.g. specific rows, or "overlap", or "all"
					s.setRows((String)jobj.get("rows"));
				}
				// mapping
				Map rowMapping = new ArrayMap(jobj);
				rowMapping.remove("name"); rowMapping.remove("url"); rowMapping.remove("rows");
				s.setMapping(rowMapping);
			}
			return s;
		}
	};

	public static Parser<ImportRowCommand> importRow = new Ref("importRow");
	
	/**
	 * E.g. import count of sales by month from SalesForce
	 */
	PP<ImportRowCommand> _importRow1 = (PP<ImportRowCommand>) new PP<ImportRowCommand>(
			seq(lit("import"), opt(cache), 
					space, lit("by month","aggregate").label("slicing"), 
					space, LangNum.num, // the formula, applied to the csv being imported, e.g. count(Amount)
					opt(seq(lit(" using "), jsonLike)), 
					lit(" from "), LangMisc.urlOrFile)
			) {
		protected ImportRowCommand process(ParseResult<?> r) {			
			AST<MatchResult> psrc = r.getNode(LangMisc.urlOrFile);
			ImportRowCommand s = new ImportRowCommand(psrc.parsed());
			// the formula
			AST<Formula> fNode = r.getNode(LangNum.num);
			s.formula = fNode.getX();
			// cache settings?
			AST<String> isFresh = r.getNode(cache);
			if (isFresh != null) {
				s.setCacheDt(TimeUtils.NO_TIME_AT_ALL); 
			}
			// slicing -- monthly or one-aggregate number?
			String slicing = (String) r.getNode("slicing").getX();
			s.setSlicing(slicing);
			// extra info
			AST<Map> _jobj = r.getNode(jsonLike);
			if (_jobj!=null) {
				Map jobj = _jobj.getX();
				// meta data
				if (jobj.containsKey("name")) {
					s.name = (String) jobj.get("name");
				}
				if (jobj.containsKey("url")) {
					s.url = (String) jobj.get("url");
				}
				// mapping
				s.setMapping(jobj);
			}
			return s;
		}
	};

	
	/**
	 * E.g. import count of sales by month from SalesForce
	 */
	PP<ImportRowCommand> _importRow2 = (PP<ImportRowCommand>) new PP<ImportRowCommand>(
			seq(lit("import"), 
//					opt(cache), 
					space, LangCellSet.cellSet1Row, 
					// NB: for some reason cellSet1Row is grabbing the space, so put optSpace here for flex
					optSpace, lit("from "), LangMisc.urlOrFile,
					optSpace, opt(jsonLike)
					)
			) {
		protected ImportRowCommand process(ParseResult<?> r) {			
			AST<MatchResult> psrc = r.getNode(LangMisc.urlOrFile);
			ImportRowCommand s = new ImportRowCommand(psrc.parsed());
			// the formula
			AST<CellSet> fNode = r.getNode(LangCellSet.cellSet1Row);
			CellSet sel= (CellSet) fNode.getX();
			s.formula = new BasicFormula(sel);
			s.setSlicing("none");
			// cache settings?
			AST<String> isFresh = r.getNode(cache);
			if (isFresh != null) {
				s.setCacheDt(TimeUtils.NO_TIME_AT_ALL); 
			}
			// extra info
			AST<Map> _jobj = r.getNode(jsonLike);
			if (_jobj!=null) {
				Map jobj = _jobj.getX();
				if (jobj.containsKey("name")) {
					s.name = (String) jobj.get("name");
				}
				if (jobj.containsKey("url")) {
					s.url = (String) jobj.get("url");
				}
			}
			return s;
		}
	};

	Parser<ImportRowCommand> _importRowEither = first(_importRow1, _importRow2).label("importRow");

	/**
	 * @see #importCommand
	 */
	PP<ExportCommand> _exportCommand = new PP<ExportCommand>(
			seq(lit("export"), 
					opt(lit(" all"," overlap", " annual").label("whichRows")), 	
					opt(seq(word(" scenario"), space, ref(LangCellSet.ROW_NAME))),
					opt(seq(word(" from"), space, LangTime.time)),
					lit(":"), 
					optSpace, LangMisc.urlOrFile, 
					optSpace, opt(jsonLike))
			) {
		protected ExportCommand process(ParseResult<?> r) {			
			AST<MatchResult> psrc = r.getNode(LangMisc.urlOrFile);
			ExportCommand s = new ExportCommand(psrc.parsed());
			s.overwrite = true;						
//			// rows: all / overlap? (can also be set in the json key:value)
//			AST<String> whichRows = r.getNode("whichRows");
//			if (whichRows != null) {
//				s.setRows(whichRows.getX()); 
//			}			
			AST rn = r.getNode(LangCellSet.ROW_NAME);
			if (rn!=null) {
				s.setScenarios(Arrays.asList(rn.parsed()));
			} else {
				Log.e("no scenario");
			}
			AST<TimeDesc> ft = r.getNode(LangTime.time);
			if (ft!=null) {
				TimeDesc from = ft.getX();
				s.setFrom(from);
				Log.e(ft);
//				s.setFrom(ft);
			}
			// extra info
			AST<Map> _jobj = r.getNode(jsonLike);
			if (_jobj!=null) {
				Map jobj = _jobj.getX();
				if (jobj.containsKey("name")) {
					s.name = (String) jobj.get("name");
				}
				if (jobj.containsKey("format")) {
					s.format = (String) jobj.get("format");
				}
				if (jobj.containsKey("url")) {
					s.url = (String) jobj.get("url");
				}
				if (jobj.containsKey("rows")) {
					// e.g. specific rows, or "overlap", or "all"
//					s.setRows((String)jobj.get("rows"));
				}
				// mapping
				Map rowMapping = new ArrayMap(jobj);
				rowMapping.remove("name"); rowMapping.remove("url"); rowMapping.remove("rows");
//				s.setMapping(rowMapping);
			}
			return s;
		}
	}.label("exportCommand");
	public static Parser<ExportCommand> exportCommand = new Ref("exportCommand"); // is this needed??

	
	/**
	 * start / end
	 */
	Parser<Settings> planSettings = seq(
			first(startEndSetting, samplesSetting, columnSettings),			
			opt(space), opt(comment)
			);

	public PP<AnnualRule> annual = new PP<AnnualRule>(seq(
			lit("annual "), LangCellSet.cellSet1Row, lit(":"),optSpace,
			lit("average","sum","previous","off").label("op"),
			opt(seq(lit(" weighted by "), LangCellSet.cellSet1Row)),
			opt(comment)
			)) {
		protected AnnualRule process(ParseResult<?> r) {			
			AST<CellSet> _rn = r.getNode(LangCellSet.cellSet1Row);
			CellSet rn = _rn.getX();
			String op = (String) r.getNode("op").getX();
			AnnualRule ar = new AnnualRule((RowName) rn, op, r.toString());
			if (r.toString().contains("weighted")) {
				throw new TodoException();
			}
			return ar;
		}
	}.label("annual");
	
}

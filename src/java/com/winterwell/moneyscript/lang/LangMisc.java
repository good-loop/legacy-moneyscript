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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;

import com.winterwell.moneyscript.lang.cells.Filter;
import com.winterwell.moneyscript.lang.time.DtDesc;
import com.winterwell.moneyscript.lang.time.LangTime;
import com.winterwell.moneyscript.lang.time.TimeDesc;
import com.winterwell.nlp.simpleparser.AST;
import com.winterwell.nlp.simpleparser.PP;
import com.winterwell.nlp.simpleparser.ParseFail;
import com.winterwell.nlp.simpleparser.ParseResult;
import com.winterwell.nlp.simpleparser.Parser;
import com.winterwell.nlp.simpleparser.Parsers;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;
import com.winterwell.utils.web.WebUtils;

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

	protected static Parser<MatchResult> urlOrFile = Parsers.regex(WebUtils.URL_REGEX.pattern());
	

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
			seq(lit("start", "end"), lit(":"), optSpace, LangTime.time)
			) {
		protected Settings process(ParseResult<?> r) {
			Settings s = new Settings();
			String keyword = (String) r.getLeafValues().get(0);
			AST<TimeDesc> ptime = r.getNode(LangTime.time);
			TimeDesc timeDesc = ptime.getX();
			Time time = timeDesc.getTime();
			if ("start".equals(keyword)) {
				s._start = time;
			}
			if ("end".equals(keyword)) {
				s._end = time;
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
	Parser<Map> jsonLikeKeyVal = new PP<Map>(regex("\"?[a-zA-Z0-9]+\"?:\\s*[^},]+")) {
		@Override
		protected Map process(ParseResult<?> r) throws ParseFail {
			String kv = r.parsed();
			int i = kv.indexOf(':');
			// TODO handle "s
			String k = kv.substring(0, i).trim();
			String v = kv.substring(i+1).trim();
			return new ArrayMap(k,v);
		}		
	};
	
	Parser<Map> jsonLike = new PP<Map>(
			seq(lit("{"), chain(jsonLikeKeyVal, seq(optSpace,lit(","),optSpace)), lit("}"))
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
	};
	
	/**
	 * e.g. import actuals from a csv
	 * 
	 * TODO other blend modes?
	 */
	PP<ImportCommand> importCommand = new PP<ImportCommand>(
			seq(lit("import"), opt(cache), lit(":"), optSpace, LangMisc.urlOrFile, optSpace, opt(jsonLike))
			) {
		protected ImportCommand process(ParseResult<?> r) {			
//			String keyword = (String) r.getLeafValues().get(1);
			AST<MatchResult> psrc = r.getNode(LangMisc.urlOrFile);
			ImportCommand s = new ImportCommand(psrc.parsed());
			s.overwrite = true;			
			// cache settings?
			AST<String> isFresh = r.getNode(cache);
			if (isFresh != null) {
				s.cacheDt = TimeUtils.NO_TIME_AT_ALL; 
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
	
	/**
	 * start / end
	 */
	Parser<Settings> planSettings = seq(
			first(startEndSetting, samplesSetting, columnSettings),			
			opt(comment)
			);
	
}

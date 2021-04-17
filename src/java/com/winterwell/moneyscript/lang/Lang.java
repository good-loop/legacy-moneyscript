package com.winterwell.moneyscript.lang;

import static com.winterwell.nlp.simpleparser.Parsers.first;
import static com.winterwell.nlp.simpleparser.Parsers.lit;
import static com.winterwell.nlp.simpleparser.Parsers.opt;
import static com.winterwell.nlp.simpleparser.Parsers.optSpace;
import static com.winterwell.nlp.simpleparser.Parsers.ref;
import static com.winterwell.nlp.simpleparser.Parsers.seq;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.winterwell.maths.NoDupes;
import com.winterwell.moneyscript.lang.bool.LangBool;
import com.winterwell.moneyscript.lang.cells.CellSet;
import com.winterwell.moneyscript.lang.cells.CurrentRow;
import com.winterwell.moneyscript.lang.cells.LangCellSet;
import com.winterwell.moneyscript.lang.cells.LangFilter;
import com.winterwell.moneyscript.lang.cells.RowName;
import com.winterwell.moneyscript.lang.cells.Scenario;
import com.winterwell.moneyscript.lang.num.Formula;
import com.winterwell.moneyscript.lang.num.LangNum;
import com.winterwell.moneyscript.lang.time.LangTime;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.BusinessContext;
import com.winterwell.moneyscript.output.Row;
import com.winterwell.nlp.simpleparser.AST;
import com.winterwell.nlp.simpleparser.IDebug;
import com.winterwell.nlp.simpleparser.PP;
import com.winterwell.nlp.simpleparser.ParseExceptions;
import com.winterwell.nlp.simpleparser.ParseFail;
import com.winterwell.nlp.simpleparser.ParseResult;
import com.winterwell.nlp.simpleparser.ParseState;
import com.winterwell.nlp.simpleparser.Parser;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Cache;
import com.winterwell.utils.containers.Slice;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.XStreamUtils;


/**
 * 
 * @testedby  LangTest}
 * @author daniel
 *
 */
public class Lang {
	
	public static final String[] keywords =
			// filters
			("from to above below at £ $ except "
			// times
			+"start end now for at "
			+"per over "
			+"year quarter month day hour minute years quarters months days hours minutes "
			// finance
			+""
			// maths
			+"min max average sum "
			// prob
			+"or "
			// logic
			+"if then else and "
			// meta
			+"show hide plot off "
			// IO
			+"import fresh "
			// global variables
			+"row column previous "
			// structural
			+"annual "
			// future use: 
//			+"depreciated vat tax "
			+"sheet scenario "
			+"number filter on all in once every each unit when row column rows columns time Time timestep grow linear exponential"
			).split("\\s+");			
	
	
	final LangMisc langMisc = new LangMisc();
	public final LangNum langNum = new LangNum();
	public final LangBool langBool = new LangBool();
	public final LangCellSet langCellSet = new LangCellSet();
	public final LangTime langTime = new LangTime();
	public final LangFilter langFilter = new LangFilter();
	
	/**
	 * Any one line. This is the top level parser.
	 */
	public Parser line;
	
	Parser na = lit("na");
	
	Parser<Rule> commentRow = new PP<Rule>(LangMisc.comment) {
		protected Rule process(ParseResult r) {
			return new DummyRule(null, r.parsed());
		}
	}.eg("// foo");
	
	/**
	 * group rows are just a row-name without a formula.
	 * Scenarios are groups created with the keyword `scenario`
	 */
	Parser<Rule> groupRow = new PP<Rule>(
			seq(LangMisc.indent, opt(lit("scenario ").label("scenario")), 
					ref(LangCellSet.ROW_NAME), lit(":"), optSpace, opt(na), opt(LangMisc.comment))) 
	{
		protected Rule process(ParseResult r) {			
			AST<Number> indentAst = r.getNode(LangMisc.indent);
			int ind = indentAst.getX().intValue();
			AST rn = r.getNode(LangCellSet.ROW_NAME);
			String rname = rn.parsed();
			boolean scenario = r.getNode("scenario") != null;			
			GroupRule gr = scenario? new ScenarioRule(new Scenario(rname), ind)
							: new GroupRule(new RowName(rname), ind);
			AST rna = r.getNode(na);
			if (rna != null) {
				gr.na = true;
			}
			return gr;
		}
	}.eg("Staff: na");
	
	Parser ruleBody = first(langNum.numList, LangNum.num, 
							langNum.compoundingFormula, 						 
							LangMisc.meta, 
							LangMisc.importRow, LangMisc.exportRow, 
							LangMisc.css, langTime.when)
					.label("ruleBody")
					.setDebug(new IDebug<ParseState>() {public void call(ParseState state) {
							assert state != null;
						}});
	
	/**
	 * What unit is this row?
	 */
	public static Parser<String> unit = seq(lit("("), lit("%","£","$").label("unit"), lit(")"))
			.eg("(%)");
	
	Parser<Rule> rule = new PP<Rule>(seq( 
			LangMisc.indent,
			LangCellSet.cellSet,
			opt(unit),
			lit(":"), optSpace, 
			ruleBody,
			opt(LangMisc.comment),
			optSpace)
	) {
		protected Rule process(ParseResult<?> r) {
			int ind = r.getNode(LangMisc.indent).getX().intValue();
			AST<CellSet> s = r.getNode(LangCellSet.cellSet);
			CellSet sel = (CellSet) s.getX();
			AST<?> rb = r.getNode(ruleBody);			
			assert rb != null : r;
			// record the comment if there was one
			AST astComment = r.getNode(LangMisc.comment);
			String comment = astComment==null? null : ""+astComment.getValue();
			AST<String> u = r.getNode("unit");
			// a formula?
			Object rbx = rb.getX();			
			if (rbx instanceof Formula) {
				Rule _rule = new Rule(sel, (Formula) rbx, r.parsed(), ind).setComment(comment);
				if (u != null) {
					_rule.setUnit(u.getX());
				}
				return _rule;
			}
			// a list of values
			if (rbx instanceof List) {
				Rule _rule = new ListValuesRule(sel, (List<Formula>) rbx, r.parsed(), ind).setComment(comment);
				if (u != null) {
					_rule.setUnit(u.getX());
				}
				return _rule;
			}
			// a rule? (importRow does this -- it probably should return a formula instead)
			if (rbx instanceof Rule) {
				Rule _rule = (Rule) rbx;
				_rule.setSelector(sel);
				_rule.indent = ind;
				_rule.setComment(comment);
				return _rule;
			}
			// meta rule?
			AST<String> m = rb.getNode(LangMisc.meta);
			if (m != null) {
				return new MetaRule(sel, m.parsed(), r.parsed()).setComment(comment);
			}
			// css?
			AST c = rb.getNode(LangMisc.css);			
			if (c != null) {
				return new StyleRule(sel, (String) rbx, r.parsed(), ind).setComment(comment);
			}			
			Log.d("DummyRule used for: "+r.parsed());
			return new DummyRule(sel, r.parsed()).setComment(comment);
		};
	};		

	
	public Lang() {
		line = first(
				// core language
				rule, 
				groupRow, 
				commentRow,
				// settings
				langMisc.annual,
//				langMisc.columnSettings, 
				langMisc.planSettings,
				langMisc.importCommand
				);
		line.label("line");
	}

	/**
	 * HACK speed up repeated parsing with a cache
	 */
	private static Cache<String, Object> cache = new Cache(5000);
	
	public final static void setCache(Cache<String, Object> cache) {
		Lang.cache = cache;
	}
	

	public Rule parseLine(String scriptLine, Business b) {
		Object r = null;
		// HACK cache for speed
		if (cache != null) {
			r = cache.get(scriptLine);			
		}
		if (r==null) {
			ParseResult pr = line.parse(scriptLine);
			if (pr==null) return null;
			r = pr.ast.getX();
			cache.put(scriptLine, r);
		} else {
			// cache hit!
		}
		// Parsed lines are not quite immutable :(
		// So clear stale data
		if (r instanceof IReset) {
			((IReset) r).reset();
		}
		
		// import?
		if (r instanceof ImportCommand) {			
			b.addImportCommand((ImportCommand) r);
			return (Rule) r;
		}
		// export?
		if (r instanceof ExportCommand) {			
			b.addExportCommand((ExportCommand) r);
			return new DummyRule(null, scriptLine);
		}
		// A rule (the normal case)
		if (r instanceof Rule) {
			return (Rule) r;
		}
		// settings?
		if (r instanceof Settings) {
			Settings settings = (Settings) r;
			Settings oldSettings = b.getSettings();
			Settings merged = oldSettings.merge(settings);
			b.setSettings(merged);
			return new DummyRule(null, scriptLine);
		}		
		// fail (may be analysed for why later)
		return null;
	}

	public Business parse(String script) throws ParseExceptions {
		assert script != null;
		Business b = new Business();
		BusinessContext.setBusiness(b);
		if (Utils.isBlank(script)) {
			return b;
		}
		String[] lines = StrUtils.splitLines(script);
		int ln = 0;
		// get title? (any other header stuff?)
		while(ln<lines.length) {
			String line = lines[ln];
			if (Utils.isBlank(line)) {
				ln++;
				continue;
			}
			if (line.indexOf(':') == -1 || line.startsWith("title:")) {
				b.title = line;
				ln++;
			}
			break;
		}
		// track the group we're in to group rules
		List<Group> groupStack = new ArrayList();

		List<ParseFail> errors = new ArrayList<ParseFail>();
		// ...do the actual parse
		List<Rule> rules = parse2_rulesFromLines(lines, ln, errors, b);		
		// make rows + group
		parse3_addRulesAndGroupRows(b, groupStack, rules);

		List<ParseFail> unref = parse4_checkReferences(b);
		errors.addAll(unref);
		
		List<ParseFail> dupes = parse5_checkDuplicates(b);
		errors.addAll(dupes);
		
		if ( ! errors.isEmpty()) {
			throw new ParseExceptions(errors);
		}
		
		return b;
	}

	/**
	 * In a spreadsheet you can have two identical rows. That would be a mistake here.
	 * Use e.g. Sales Person A, Sales Person B to distinguish them
	 * @param b
	 * @return
	 */
	private List<ParseFail> parse5_checkDuplicates(Business b) {
		List<ParseFail> dupes = new ArrayList();
		Set<Rule> rules = b.getAllRules();
		NoDupes<String> cellsets = new NoDupes<>();
		for (Rule r : rules) {
			if (r instanceof ImportRowCommand || r instanceof StyleRule) {
				continue;
			}
			if (r.formula==null) {
				// what is this??
				System.out.println(r);
				continue;
			}
			if (r.getSelector()==null) {
				System.out.println(r);
				continue;
			}
			CellSet cellset = r.getSelector();
			// NB: overlaps between scenarios are fine
			String cs = r.getScenario()+XStreamUtils.serialiseToXml(cellset);
			if ( ! cellsets.isDuplicate(cs)) {
				continue; // all good
			}
			ParseFail pf = new ParseFail(new Slice(r.src), 
				"This rule overlaps with another rule for: "+cellset.getSrc());
			pf.lineNum = r.lineNum;
			dupes.add(pf);
		}
		return dupes;
	}


	private void parse3_addRulesAndGroupRows(Business b, List<Group> groupStack, List<Rule> rules) {
		for (Rule rule : rules) {
			if (rule instanceof DummyRule || rule instanceof ImportCommand)  {
				// HACK imports dont have rows per-se. But ImportRowCommand does
				if ( ! (rule instanceof ImportRowCommand)) {
					continue;
				}
			}

			CellSet selector = rule.getSelector();
			if (selector==null) {
				selector = new CurrentRow(null); // is this always OK?? How do grouping rules behave??
			}
			Set<String> rowNames = selector.getRowNames(null);
			if (rule instanceof ScenarioRule) {
				// HACK don't add a row for scenario X
				rowNames = Collections.emptySet();
			} else {
				assert ! rowNames.isEmpty() : rule;
			}
			// the rows named in this rule's selector
			// Make sure they exist
			List<Row> rows = new ArrayList<Row>();
			boolean isNewRow = false;
			for (String rn : rowNames) {
				Row row = b.getRow(rn);
				if (row==null) {
					row = new Row(rn);
					b.addRow(row);
					isNewRow = true;
				}
				rows.add(row);
			}	
			
			// add the rule
			b.addRule(rule);
			
			// Grouping by groupStack
			// NB: only if there's one row (i.e. not for no-row comments, or a multi-row rule -- Do we have those?)
			Group group = parse4_addRulesAndGroupRows2_group(rule, rows);
			if (group==null) {
				continue;
			}
			// Manage the group stack
			Group parent = null;
			while( ! groupStack.isEmpty()) {
				Group last = groupStack.get(groupStack.size() - 1);
				if (last.indent < group.indent) {
					// group found - stop here
					parent = last;
					break;
				}
				// pop a now closed group
				groupStack.remove(groupStack.size() - 1);
			}
			// the current rule is the new group
			groupStack.add(group);
			group.setParent(parent);
			
			// add this row0 to a group?
			if (parent==null) {
				continue;
			}			
			// the first appearance wins -- later rules wont change the parent
			if (isNewRow) {
				if (group.byRow != null) {
					Row row0 = group.byRow;
					row0.setParent(parent.byRow);
					b.reorderRows();
				} else {
					Log.d("Lang", "huh "+rule);
				}
			} else {
				// a later group - it can't claim the row fully
				// Hm: for e.g. the `Pay Rise` use case, it'd be nice to see the effect of those rules broken out.
				// But fiddly!	
				// We could have Numerical's track their history, and what each Rule contributes
				// (perhaps just for some Rules)
			} // ./grouping	
			
			// rule grouping eg for scenarios (which apply at the rule level not the row level)
			parse4_addRulesAndGroupRows2_setRuleGroup(rule, parent);			
		}
	}

	/**
	 * 
	 * @param rule
	 * @param rows
	 * @return Can return null
	 */
	private Group parse4_addRulesAndGroupRows2_group(Rule rule, List<Row> rows) {
		if (rule instanceof ScenarioRule) {
			return new Group(rule.getScenario(), rule.indent);
		}
		if (rows.size() != 1) {
			return null;
		}
		Row row0 = rows.get(0);		
		Group group = new Group(row0, rule.indent);
		return group;
	}


	/**
	 * rule grouping eg for scenarios (which apply at the rule level not the row level)
	 * @param rule2
	 * @param parent
	 */
	private void parse4_addRulesAndGroupRows2_setRuleGroup(Rule newRule, Group parent) {		
		// check up the stack
		while(parent!=null) {
			if (parent.byScenario != null) {
				newRule.setScenario(parent.byScenario);
			}
			parent = parent.parent;
		}
	}


	/**
	 * 
	 * @param lines
	 * @param ln starting line number (eg a header may have been parsed already)
	 * @param errors
	 * @return
	 */
	private List<Rule> parse2_rulesFromLines(String[] lines, int ln, List<ParseFail> errors, Business b) {
		List<Rule> rules = new ArrayList();
		for (; ln<lines.length; ln++) {
			String lineln = lines[ln];
			if (Utils.isBlank(lineln)) {
				continue;			
			}
			
			// Parse a line of script
			Rule rule = null;
			try {
				// NB: can return null on error
				rule = parseLine(lineln, b);
			} catch(Throwable ex) {
				Log.w("Lang.parse", ex);
			}
			if (rule == null) {	// error :(
				ParseFail pf = parse2_rulesFromLines2_fail(lineln, ln);
				errors.add(pf);
				continue;
			}
			
			rule.lineNum = ln;
			rules.add(rule);

			// HACK: Process import of m$
			if (rule instanceof ImportCommand && rule.src.endsWith("ms") || rule.src.endsWith("ms")) {
				ImportCommand ic = (ImportCommand) rule;
				Business b2 = ic.runImportMS(this);
				Set<Rule> importedRules = b2.getAllRules();
				Settings settings2 = b2.getSettings();
				rules.addAll(importedRules);
				// merge (our settings take precedence)
				Settings s3 = settings2.merge(b.getSettings());
				b.setSettings(s3);
			}			
		}
		return rules;
	}


	private ParseFail parse2_rulesFromLines2_fail(String text, int lineNum) {
		ParseFail e = ParseFail.getParseFail();
		if (e == null) {
			e = new ParseFail(new Slice(text), null);
		}
		// look for common problems
		if (e.getMessage()==null) {
			if ( ! text.contains(":")) {
				e.setMessage("No colon found. Rules should be of the form `CellDescription: Formula`");
			}
		}
		e.lineNum = lineNum;
		return e;
	}

	/**
	 * Check the references in a formula against the row names in the business
	 * @param b
	 * @param unref
	 * @return 
	 */
	private List<ParseFail> parse4_checkReferences(Business b) {
		List<ParseFail> unref = new ArrayList();
		// row names
		HashSet<String> names = new HashSet<String>();
		// To handle case typos - name for strongly-canonicalised name - e.g. {danw: "Dan W"}
		HashMap<String,String> similarNames = new HashMap<String,String>();
		for(Row r : b.getRows()) {
			String name = r.getName();
			names.add(name);
			String n2 = parse4_checkReferences2_stripNameDown(name);			
			similarNames.put(n2,name);
		}
		
		Set<Rule> rules = b.getAllRules();		
		for (Rule r : rules) {
			if (r.formula == null) continue;
			if (r instanceof ImportRowCommand) {
				continue; // allow references to imported rows
			}
			Set<String> vars;
			try {
				vars = r.formula.getRowNames(null);
			} catch (UnsupportedOperationException e) {
				// e.g. CurrentRow
				continue;
			}
			for (String var : vars) {
				String v2 = parse4_checkReferences2_stripNameDown(var);
				String couldBe = similarNames.get(v2);
				boolean exists = names.contains(var);
				// undefined?!
				if ( ! exists) {									
					String msg = "`"+var+"` is not defined.";
					if (couldBe!=null) msg += " Did you mean `"+couldBe+"`?"; 
					ParseFail pf = new ParseFail(new Slice(r.src), msg);
					pf.lineNum = r.lineNum;
					unref.add(pf);
					continue;
				}
				// confusable?!
				if (couldBe != null && ! var.equals(couldBe)) {
					String msg = "`"+var+"` could be confused with `"+couldBe+"`.";
					ParseFail pf = new ParseFail(new Slice(r.src), msg);
					pf.lineNum = r.lineNum;
					unref.add(pf);
				}
			}
		}
		return unref;
	}

	private String parse4_checkReferences2_stripNameDown(String name) {
		return StrUtils.toCanonical(name).replaceAll("\\W", "");
	}
	
}
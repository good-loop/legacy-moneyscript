package com.winterwell.moneyscript.lang;

import static com.winterwell.nlp.simpleparser.Parsers.first;
import static com.winterwell.nlp.simpleparser.Parsers.lit;
import static com.winterwell.nlp.simpleparser.Parsers.opt;
import static com.winterwell.nlp.simpleparser.Parsers.optSpace;
import static com.winterwell.nlp.simpleparser.Parsers.seq;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.winterwell.moneyscript.data.PlanSheet;
import com.winterwell.moneyscript.lang.bool.LangBool;
import com.winterwell.moneyscript.lang.cells.CellSet;
import com.winterwell.moneyscript.lang.cells.ChainFilter;
import com.winterwell.moneyscript.lang.cells.CurrentRow;
import com.winterwell.moneyscript.lang.cells.Filter;
import com.winterwell.moneyscript.lang.cells.FilteredCellSet;
import com.winterwell.moneyscript.lang.cells.LangCellSet;
import com.winterwell.moneyscript.lang.cells.LangFilter;
import com.winterwell.moneyscript.lang.cells.RowName;
import com.winterwell.moneyscript.lang.cells.RowNameWithFixedVariables;
import com.winterwell.moneyscript.lang.cells.RowSplitCellSet;
import com.winterwell.moneyscript.lang.cells.Scenario;
import com.winterwell.moneyscript.lang.cells.SetVariable;
import com.winterwell.moneyscript.lang.num.BasicFormula;
import com.winterwell.moneyscript.lang.num.Formula;
import com.winterwell.moneyscript.lang.num.LangNum;
import com.winterwell.moneyscript.lang.num.ColVar;
import com.winterwell.moneyscript.lang.num.VariableDistributionFormula;
import com.winterwell.moneyscript.lang.time.LangTime;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.BusinessContext;
import com.winterwell.moneyscript.output.Row;
import com.winterwell.moneyscript.output.RowVar;
import com.winterwell.moneyscript.output.VarSystem;
import com.winterwell.nlp.simpleparser.AST;
import com.winterwell.nlp.simpleparser.IDebug;
import com.winterwell.nlp.simpleparser.PP;
import com.winterwell.nlp.simpleparser.ParseExceptions;
import com.winterwell.nlp.simpleparser.ParseFail;
import com.winterwell.nlp.simpleparser.ParseResult;
import com.winterwell.nlp.simpleparser.ParseState;
import com.winterwell.nlp.simpleparser.Parser;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Cache;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.containers.Slice;
import com.winterwell.utils.containers.Tree;
import com.winterwell.utils.log.Log;


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
			+"start end now for at hide "
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
			+"show hide plot off only " // TODO "only" as a marker for "only this rule", like css !important
			// IO
			+"import fresh "
			// global variables
			+"row column previous "
			// structural
			+"annual split "
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
	
	/**
	 * Means "skip in calculations"
	 */
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
//					ref(LangCellSet.ROW_NAME),
					LangCellSet.cellSet, // allow "Row" or "Row from next year"
					lit(":"), 
					optSpace, opt(na), 
					optSpace,
					opt(LangNum.hashTag),
					optSpace,
					opt(LangMisc.comment))
			)
	{
		protected Rule process(ParseResult r) {			
			AST<Number> indentAst = r.getNode(LangMisc.indent);
			int ind = indentAst.getX().intValue();
			AST<CellSet> rn = r.getNode(LangCellSet.cellSet);
			CellSet row = rn.getX();
			String rname = rn.parsed();
			boolean scenario = r.getNode("scenario") != null;			
			GroupRule gr;
			if (scenario) {
				gr = new ScenarioRule(new Scenario(rname), ind);
			} else {		
				gr = new GroupRule(row, ind);
			}
			AST rna = r.getNode(na);
			if (rna != null) {
				gr.na = true;
			}
			AST hashtag = r.getNode(LangNum.hashTag);
			if (hashtag != null) {				
				Object h = hashtag.getX();
				gr.setTag((String)h);
			}
			return gr;
		}
	}.eg("Staff: na").eg("London: #uk // meh");
	
	Parser ruleBody = first(langNum.numList, LangNum.num, 
							langNum.compoundingFormula, 						 
							LangMisc.meta, 
							LangMisc.importRow, 
							LangMisc.css, langTime.when)
					.label("ruleBody")
					.setDebug(new IDebug<ParseState>() {public void call(ParseState state) {
							assert state != null;
						}});
	
	/**
	 * What unit is this row?
	 * HACK: includes CPM as a unit!
	 */
	public static Parser<String> unit = seq(lit("("), lit("%","£","$","CPM").label("unit"), lit(")"))
			.eg("(%)");
	
	static final Parser<String> only = lit(" only");
	
	Parser<Rule> rule = new PP<Rule>(seq( 
			LangMisc.indent,
			LangCellSet.cellSet,
			opt(unit),
			lit(":"), optSpace, 
			ruleBody,
			opt(only),
			optSpace,
			opt(LangNum.hashTag),
			optSpace,
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
			// unit?
			AST<String> u = r.getNode("unit");
			// hashtag?
			AST<String> astTag = r.getNode(LangNum.hashTag);
			String tag = null;
			if (astTag!=null) {
				tag = astTag.getX();			
			}
			boolean isOnly = r.getNode(only) != null;
			// a formula?
			Rule _rule = null;
			Object rbx = rb.getX();			
			if (rbx instanceof Formula) {
				_rule = new Rule(sel, (Formula) rbx, r.parsed(), ind);
			} else if (rbx instanceof List) { 
				// list of values
				_rule = new ListValuesRule(sel, (List<Formula>) rbx, r.parsed(), ind);
			}
			// a rule? (importRow does this -- it probably should return a formula instead)
			if (rbx instanceof Rule) {
				_rule = (Rule) rbx;
				_rule.setSelector(sel);
				_rule.indent = ind;
			}
			if (_rule != null) {
				if (u != null) {
					_rule.setUnit(u.getX());
				}
				if (tag !=null) {
					_rule.setTag(tag);
				}
				if (comment!=null) {
					_rule.setComment(comment);
				}
				if (isOnly) {
					_rule.setOnly(isOnly);
				}
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

	
	/**
	 * The standard language - This is a cheap operation 
	 */
	public Lang() {
		line = first(
				// core language
				langMisc.annual,
				rule, 
				groupRow, 
				commentRow,
				// settings				
//				langMisc.columnSettings, 
				langMisc.planSettings,
				langMisc.importCommand, langMisc.exportCommand
				);
		line.label("line");
	}

	/**
	 * HACK speed up repeated parsing with a cache.
	 * Things in the cache must be copied to avoid shared-state bugs.
	 */
	private static Cache<String, Object> cache = new Cache(5000);
	
	public final static void setCache(Cache<String, Object> cache) {
		Lang.cache = cache;
	}
	

	/**
	 * Parse - uses a cache for spped
	 * @param scriptLine
	 * @param b
	 * @return
	 */
	public Rule parseLine(String scriptLine, Business b) {
		Object r = null;
		// HACK cache for speed
		if (cache != null) {
			r = cache.get(scriptLine);
			r = Utils.copy(r); // copy to avoid cache vs edit issues
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
		
		// import / export?
		if (r instanceof ExportCommand) {
			Log.e("Lang", "ExportCommand should be in PlanDoc now");
//			b.addExportCommand((ExportCommand) r);
			return (Rule) r;
		}
		if (r instanceof ImportCommand) {			
			b.addImportCommand((ImportCommand) r);
			return (Rule) r;
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

	/**
	 * @deprecated 
	 * @param script
	 * @return
	 * @throws ParseExceptions
	 */
	public Business parse(String script) throws ParseExceptions {
		return parse(Arrays.asList(new PlanSheet(script)), null);
	}
	
	/**
	 * 
	 * @param script
	 * @param settings Can be null
	 * @return
	 * @throws ParseExceptions
	 */
	public Business parse(List<PlanSheet> script, Settings settings) throws ParseExceptions {
		assert script != null;
		Business b = new Business();
		if (settings != null) {
			b.setSettings(settings);
		}
		BusinessContext.setBusiness(b);
		if (script.isEmpty()) {
			return b;
		}
		
		List<ParseFail> errors = new ArrayList<ParseFail>();
		// process each sheet
		for(PlanSheet planSheet : script) {
			if (Utils.isBlank(planSheet.getText())) continue;		
			String[] lines = StrUtils.splitLines(planSheet.getText());
			int ln = 0;
			// get title? (any other header stuff?)
			while(ln<lines.length) {
				String linen = lines[ln];
				if (Utils.isBlank(linen)) {
					ln++;
					continue;
				}
				if (linen.indexOf(':') == -1 || linen.startsWith("title:")) {
					b.title = linen;
					ln++;
				}
				break;
			}
			// track the group we're in to group rules
			List<Group> groupStack = new ArrayList(); // ??Group has a parent, so do we need this stack??
	
			// ...do the actual parse
			List<Rule> rules = parse2_rulesFromLines(lines, ln, errors, b, planSheet);
			// identify special variable value rows, e.g. "Price [Region=UK]: £1"
			parse3_identifyVariables(b, rules);
			// make rows + group
			parse3_addRulesAndGroupRows(b, planSheet, groupStack, rules);
				
			// check dupes ...and also converts RowName to ScenarioName
			List<ParseFail> dupes = parse5_checkDuplicates(b);
			errors.addAll(dupes);									
		}
		
		// check rule refs at the whole-plan level (not per sheet)
		List<ParseFail> unref = parse4_checkReferences(b);
		errors.addAll(unref);
		
		// fail?
		if ( ! errors.isEmpty()) {
			throw new ParseExceptions(errors);
		}
		
		return b;
	}

	private void parse3_identifyVariables(Business b, List<Rule> rules) {
		// add SetVariables, so we know e.g. Region can be UK|US|EU
		// and functional rows, so we know e.g. "Price" can be "Price [Region=UK]" etc		
		VarSystem vs = b.getVars();
		for (Rule r : rules) {
			CellSet selector = r.getSelector();
			Formula f = r.getFormula();
			if (selector==null || f==null) continue;
			// variable??
			if (selector instanceof RowNameWithFixedVariables) {
				Collection<SetVariable> vars = ((RowNameWithFixedVariables) selector).getVars(null);
				// this is a functional row
				String baseName = ((RowNameWithFixedVariables) selector).getBaseName();
				vs.addSwitchRow(baseName, vars);
			}
		}

		//		// todo?? upgrade seemingly simple rows if they reference a var
//		boolean checkOnceMore = true;
//		while(checkOnceMore) {
//			checkOnceMore = false;
//			for (Rule r : rules) {
//				CellSet selector = r.getSelector();
//				Formula f = r.getFormula();
//				if (selector==null || f==null) continue;				
//				if (selector.getClass() == RowName.class) {
//					// this is a functional row
//					String baseName = ((RowName) selector).getRowName();
//					vs.addSwitchRow(baseName, vars); // bleurgh - vars
//					Collection<SetVariable> hmm;
//					RowNameWithFixedVariables sel2 = new RowNameWithFixedVariables(selector.getSrc(), baseName, hmm);
//					r.setSelector(sel2);
//					checkOnceMore = true;
//				}
//			}
//		}
		Log.d("parse", "VarSystem: "+vs);
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
		Map<String,Rule> rule4filter = new HashMap<>(); // detect dupes
		for (Rule r : rules) {
			if (r instanceof ImportRowCommand || r instanceof StyleRule) {
				continue;
			}
			if (r.formula==null) {
				continue; // no formula = dont care about overlaps
			}			
			if (r.getSelector()==null) {
				continue;
			}			
			// NB: overlaps between scenarios are fine
			String cs = StrUtils.joinWithSkip(" ", r.getScenario(), r.getSelector());
			int i = cs.indexOf(':'); // pop the actual rule -- we just want the filter part
			if (i!=-1) {
				cs = cs.substring(0, i);
			}			
			// the main overlap check
			if ( ! rule4filter.containsKey(cs)) {
				rule4filter.put(cs, r);
				continue; // all good
			}
			ParseFail pf = new ParseFail(new Slice(r.src), 
				"This rule overlaps with another rule for: "+cs+" - "+rule4filter.get(cs));
			pf.lineNum = r.getLineNum();
			pf.setSheetId(r.sheetId);
			dupes.add(pf);
		}
		// handle e.g. "Price [Region=UK]" means a bald "Price" isnt allowed
		// Future TODO we could allow Price: ... as a group-level rule over all the variable-values
		for (Rule r : rules) {
			CellSet cs = r.getSelector();
			if (cs instanceof RowNameWithFixedVariables) {
				String bn = ((RowNameWithFixedVariables) cs).getBaseName();
				if (rule4filter.containsKey(bn)) {
					ParseFail pf = new ParseFail(new Slice(r.src), 
						"Having a "+cs+" rule means you cannot have a \"bald\" rule "+bn+": rule.");
					pf.lineNum = r.getLineNum();
					pf.setSheetId(r.sheetId);
					dupes.add(pf);					
				}
			}
		}
		return dupes;
	}


	private void parse3_addRulesAndGroupRows(Business b, PlanSheet planSheet, List<Group> groupStack, List<Rule> rules) {
		VarSystem vars = b.getVars();
		for (Rule rule1 : rules) {			
			String sdebug = rule1.toString();
			if (sdebug.contains("Staff from Jan 2024")) {
				System.out.println(sdebug);
			}

			if (rule1 instanceof DummyRule || rule1 instanceof ImportCommand)  {
				// HACK imports dont have rows per-se. But ImportRowCommand does
				if ( ! (rule1 instanceof ImportRowCommand)) {
					continue;
				}
			}
						
			// NB: selector may be modified later to add group-level filter
			CellSet selector = rule1.getSelector();
			if (selector==null) {
				selector = new CurrentRow(null); // is this always OK?? How do grouping rules behave??
			}
			Set<String> rowNames = selector.getRowNames(null);
			if (rule1 instanceof ScenarioRule) {
				// HACK don't add a row for scenario X
				rowNames = Collections.emptySet();
			} else {
				// are any of the references to switch-rows containing vars? If so, expand the rows
				Formula f = rule1.getFormula();
				if (f!=null) {
					List<RowVar> refs = vars.getVarRefs(f);
					if ( ! refs.isEmpty()) {
						rowNames = vars.expandRowNames(rowNames, refs);
						// NB: rowNames dont have the A=B SetVariable -- done later
					}
				}
				assert ! rowNames.isEmpty() : selector+" "+rule1;
			}
			// the rows named in this rule's selector
			// Make sure they exist
			List<Row> rows = new ArrayList<Row>();
			boolean isNewRow = false;
			for (String rn : rowNames) {
				Row row = b.getRow(rn);
				if (row==null) {
					row = new Row(rn);
					b.addRow(row, planSheet);
					isNewRow = true;
				} else {
					// make sure its part of this sheet
					b.addRow2_linkToPlanSheet(row, planSheet);
				}
				rows.add(row);				
			}
			
			// Grouping by groupStack
			// NB: only if there's one row (i.e. not for no-row comments, or a multi-row rule -- Do we have those?)
			Group group = parse4_addRulesAndGroupRows2_group(rule1, rows);
			Group parent = null;
			if (group!=null) {
				// Manage the group stack				
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
			}			
			// add this row0 to a group?
			if (parent!=null) {
				// the first appearance wins -- later rules wont change the parent
				if (isNewRow) {
					if (group.byRow != null) {
						Row row0 = group.byRow;
						row0.setParent(parent.byRow);
						b.reorderRows();
					} else {
						Log.d("Lang", "huh "+rule1);
					}
				}
				// filter?
				parse4_addRulesAndGroupRows_combinedFilters(rule1, parent); 
			} // ./parent!=null
			// add split-by rows to the new group?
			if (selector instanceof RowSplitCellSet) {
				for(Row row : rows) {
					if (row != group.byRow) {
						row.setParent(group.byRow);
					}
//					b.reorderRows(); not needed for split-by
				}
			}
			
			// rule grouping eg for scenarios (which apply at the rule level not the row level, 
			// and use the local stack not the "canonical" row tree)
			Group lastGroup = null;
			if ( ! groupStack.isEmpty()) {
				lastGroup = groupStack.get(groupStack.size() - 1);
			} else {
				Log.d("Lang", "Odd! empty groupStack "+rule1);
			}
			rule1 = parse4_addRulesAndGroupRows2_setScenario(rule1, lastGroup);			

			// add the rule
			b.addRule(rule1, rows);
		}
	}

	
	/**
	 * Combine any group level filter with any filter on the row
	 * @param rule
	 * @param parent
	 */
	private void parse4_addRulesAndGroupRows_combinedFilters(Rule rule, Group parent) {
		if (rule.toString().contains("Sales Team") || rule.toString().contains("Karim")
				|| rule.toString().contains("UK Staff")) {
			System.out.println(rule);
		}
		if (parent.rule == null) { 
			return;
		}
		GroupRule gr = parent.rule;
		// hashtag?
		if (gr.getTag() != null) {
			if (rule.getTag()==null) {
				rule.setTagFromParent(gr.getTag());
			} else {
				// rule-specific hashtag overrides a group level one
			}
		}
		// filter
		CellSet groupSelector = gr.getSelector();
		CellSet gs = groupSelector;
		List<Filter> filters = new ArrayList();
		while(gs instanceof FilteredCellSet) {
			Filter filter = ((FilteredCellSet) gs).getFilter();
			// NB: we can usually ignore the base of the groupSelector, which is "this group"
			CellSet base = ((FilteredCellSet) gs).getBase();
			assert base != gs;
			gs = base; 
			filters.add(filter);
		}
		if (filters.isEmpty()) {
			return;
		}		
		Filter filter = filters.size() == 1? filters.get(0) : new ChainFilter(filters);
		CellSet ruleCells = rule.getSelector();
		FilteredCellSet fcs = new FilteredCellSet(ruleCells, filter, 
				rule.getSelector().getSrc()+" + parent: "+groupSelector.getSrc());
		rule.setSelector(fcs);
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
			if (rule.getSelector() instanceof RowSplitCellSet) {
				// carry on
			} else {
				return null;
			}
		}
		Row row0 = rows.get(0);		
		Group group = new Group(row0, rule.indent);
		if (rule instanceof GroupRule) {
			group.setRule((GroupRule) rule);
		}
		return group;
	}


	/**
	 * rule grouping eg for scenarios (which apply at the rule level not the row level)
	 * @param rule2
	 * @param parent
	 * @return 
	 */
	private Rule parse4_addRulesAndGroupRows2_setScenario(Rule newRule, Group parent) {		
		if (newRule.indent == 0) {
			return newRule;
		}
		// check up the stack
		while(parent!=null) {
			if (parent.byScenario != null) {
//				// copy because cached (copy now done when getting the rule out)
//				newRule = Utils.copy(newRule); 
				newRule.setScenario(parent.byScenario);
			}
			parent = parent.parent;
		}
		return newRule;
	}


	/**
	 * 
	 * @param lines
	 * @param ln starting line number (eg a header may have been parsed already)
	 * @param errors
	 * @param planSheet 
	 * @return
	 */
	private List<Rule> parse2_rulesFromLines(String[] lines, int ln, List<ParseFail> errors, Business b, PlanSheet planSheet) {
		List<Rule> rules = new ArrayList();
		for (; ln<lines.length; ln++) {
			String lineln = lines[ln];
			if (Utils.isBlank(lineln)) {
				continue;			
			}
			if (lineln.contains("UK Staff")) {
				System.out.println(lineln);
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
				ParseFail pf = parse2_rulesFromLines2_fail(lineln, ln, planSheet);
				errors.add(pf);
				continue;
			}
			
			rule.lineNum = ln;
			rule.sheetId = planSheet.getId();
			rules.add(rule);
			
			// HACK: Process import of m$
			if (rule instanceof ImportCommand && (rule.src.endsWith("ms") || rule.src.endsWith("ms"))) {				
				ImportCommand ic = (ImportCommand) rule;
				if (ic.getVarName() == null) {
					Business b2 = ic.runImportMS(this);
					Set<Rule> importedRules = b2.getAllRules();
					Settings settings2 = b2.getSettings();
					rules.addAll(importedRules);
					// merge (our settings take precedence)
					Settings s3 = settings2.merge(b.getSettings());
					b.setSettings(s3);
				}
			}			
		}		
		return rules;
	}


	private ParseFail parse2_rulesFromLines2_fail(String text, int lineNum, PlanSheet planSheet) {
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
		e.setSheetId(planSheet.getId());
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
			// baseName?
			if (name.indexOf("[") != -1) {
				String baseName = VarSystem.getBaseName(name);
				names.add(baseName);
			}
			String n2 = parse4_checkReferences2_stripNameDown(name);			
			similarNames.put(n2,name);
		}
		// also include scenarios
		for(Scenario r : b.getScenarios().keySet()) {
			String name = r.toString();
			names.add(name);
			String n2 = parse4_checkReferences2_stripNameDown(name);			
			similarNames.put(n2,name);
		}
		// check all rules
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
				if ( ! exists) {
					// check variables e.g. [Product in ProductMix: Product.Price]
					Formula f = r.getFormula();
					if (f != null) {
						List<String> varNames = VarSystem.getVarNames(f);
						exists = varNames.contains(var);
						if ( ! exists && var.contains(".")) { // handle e.g. Region.Price
							String var1 = var.split("\\.")[0];
							exists = varNames.contains(var1);
						}
					}
				}
				// undefined?!
				if ( ! exists) {							
					String msg = "`"+var+"` is not defined.";
					if (couldBe!=null) msg += " Did you mean `"+couldBe+"`?"; 
					ParseFail pf = new ParseFail(new Slice(r.src), msg);
					pf.lineNum = r.getLineNum();
					pf.setSheetId(r.sheetId);
					unref.add(pf);
					continue;
				}
				// confusable?!
				if (couldBe != null && ! var.equals(couldBe)) {
					String msg = "`"+var+"` could be confused with `"+couldBe+"`.";
					ParseFail pf = new ParseFail(new Slice(r.src), msg);
					pf.lineNum = r.getLineNum();
					pf.setSheetId(r.sheetId);
					unref.add(pf);
				}
				// ?? should we Rule setScenario for fast filtering? But what about "if not Scenario X" Need to create -ive scenarios
				// ?? should we mark scenario vs rule as a boolean flag in formula, for an efficiency boost later?? 
			}
		}
		return unref;
	}

	private String parse4_checkReferences2_stripNameDown(String name) {
		return StrUtils.toCanonical(name).replaceAll("\\W", "");
	}
	
}
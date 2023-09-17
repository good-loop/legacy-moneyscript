package com.winterwell.moneyscript.output;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.winterwell.gson.Gson;
import com.winterwell.maths.stats.distributions.d1.IDistribution1D;
import com.winterwell.maths.stats.distributions.d1.UniformDistribution1D;
import com.winterwell.maths.timeseries.TimeSlicer;
import com.winterwell.moneyscript.data.PlanSheet;
import com.winterwell.moneyscript.lang.CompareCommand;
import com.winterwell.moneyscript.lang.ErrorNumerical;
import com.winterwell.moneyscript.lang.ImportCommand;
import com.winterwell.moneyscript.lang.MetaRule;
import com.winterwell.moneyscript.lang.Rule;
import com.winterwell.moneyscript.lang.ScenarioRule;
import com.winterwell.moneyscript.lang.Settings;
import com.winterwell.moneyscript.lang.StyleRule;
import com.winterwell.moneyscript.lang.UncertainNumerical;
import com.winterwell.moneyscript.lang.cells.CellSet;
import com.winterwell.moneyscript.lang.cells.RowName;
import com.winterwell.moneyscript.lang.cells.Scenario;
import com.winterwell.moneyscript.lang.cells.SetVariable;
import com.winterwell.moneyscript.lang.num.Numerical;
import com.winterwell.moneyscript.webapp.GSheetFromMS;
import com.winterwell.nlp.dict.Dictionary;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.ArraySet;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.containers.ListMap;
import com.winterwell.utils.containers.Range;
import com.winterwell.utils.containers.Tree;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.web.WebUtils2;

/**
 * Top-level state object
 * @author daniel
 *
 */
public final class Business {

	
	/**
	 * HACK coordinate cell referencing during export.
	 * This is _also_ used as a debug tool during normal app use
	 */
	public transient Map<Row, GSheetFromMS> gSheetFromMSForRow;

	private Map<Scenario,Boolean> scenarios = new ArrayMap();
	
	/**
	 * Constant used to mark cells that are being evaluated.
	 */
	public static final Numerical EVALUATING = new UncertainNumerical(
			new UniformDistribution1D(new Range(-Double.MAX_VALUE, Double.MAX_VALUE)), null) {
		public String toString() {
			return "EVALUATING";
		};		
		public double doubleValue() throws IllegalStateException {
			throw new IllegalStateException("EVALUATING");
		};
		public IDistribution1D getDist() {
			throw new IllegalStateException("EVALUATING");
		}
		public Numerical sample() {
			throw new IllegalStateException("EVALUATING");
		}
	};
	
	/**
	 * Constant used to mark empty cells
	 */
	public static final Numerical EMPTY = new Numerical(0);

	List<Row> _rows = new ArrayList<Row>();

	private transient HashMap<String,Row> _row4name = new HashMap();	
	
	public Business() {
		setSettings(new Settings());		
	}
	
	public void setColumns(int n) {
		columns = new ArrayList<Col>(n);
		for(int i=1; i<=n; i++) {
			columns.add(new Col(i));	
		}
	}

	//TODO check that if you have a rule A*B, then row A and row B do exist
	void checkVariables() {
		
	}
	
	@Override
	public String toString() {
		return "Business with "+getRows();
	}
	
	public String toCSV() {
		runOnce();
		
		StringBuilder sb = new StringBuilder();		
		// times
		sb.append(", "); // blank 1st cell
		List<Col> cols = getColumns();
		for (Col col : cols) {
			Time colt = col.getTime();
			sb.append(colt.toISOStringDateOnly());
			sb.append(", ");
		}
		StrUtils.pop(sb, 2);
		sb.append(StrUtils.LINEEND);
		// values
		for(Row row : getRows()) {
			if ( ! row.isOn()) {
				continue;
			}
			sb.append(row.name);
			sb.append(", ");
			for(Col col : getColumns()) {
				Numerical v = getCellValue(new Cell(row, col));
				sb.append(v==null? "" : v.toString());
				sb.append(", ");
			}
			StrUtils.pop(sb, 2);
			sb.append(StrUtils.LINEEND);
		}
		return sb.toString();
	}

	/**
	 * Run if it hasn't been run already. Otherwise a no-op
	 */
	private void runOnce() {
		if (phase==KPhase.OUTPUT) {
			return;
		}
		run();
	}

	/**
	 * @return an map of name:rows, where each row is an array.
	 * Map starts columns:column-names
	 */
	public ArrayMap toJSON() {
		boolean incYearTotals = TUnit.MONTH.dt.equals(getSettings().timeStep);
		return toJSON2(incYearTotals);
	}
	
	public ArrayMap toJSON2(boolean incYearTotals) {
		assert phase == KPhase.OUTPUT;
		
		// parse info
		ArrayMap map = getParseInfoJson();
		
		// columns
		ArrayList<String> cols = new ArrayList<String>();
		for(Col col : getColumns()) {
			cols.add(col.getTimeDesc());
			// year total?
			if (col.getTime().getMonth() == getSettings().getYearEnd() && incYearTotals) {				
				String yr = ""+col.getTime().getYear();
				if (col.getTime().getMonth() != 12) {
					yr = (col.getTime().getYear() - 2001)+"/"+(col.getTime().getYear() - 2000);
				}
				cols.add("Total "+yr);
			}
		}
		map.put("columns", cols);
		
		// rows				
		ArrayMap datamap = new ArrayMap();
		ArrayList<String> rowNames = new ArrayList<String>();
		List<Map> jrows = new ArrayList();
		for(Row row : getRows()) {
			if ( ! row.isOn()) {
				continue;
			}			
			// The numbers
			List<Map> rowvs = row.getValuesJSON(incYearTotals);			
			datamap.put(row.name, rowvs);
			String comment = StrUtils.joinWithSkip(". ", Containers.apply(row.getRules(), Rule::getComment));
			jrows.add(new ArrayMap(
				"name", row.name,
				"comment", comment,
				"css", getCSSForRow(row)
			));			
			rowNames.add(row.getName());
		}
		map.put("rows", jrows);
		map.put("dataForRow", datamap); 
		map.put("rowsForPlansheet", rows4plansheet);
				
		// imports
		Collection<Map> importMaps = Containers.apply(getImportCommands(), ImportCommand::toJson2);
		// merge if same src
		Map<String,Map> im4src = new ArrayMap();
		Map<String,Map> ex4src = new ArrayMap();
		for (Map im : importMaps) {
			String s = ""+im.get("src");
			Map im2 = im4src.get(s);
			if (im2 == null) {
				im4src.put(s, im);
			} else {
				im2.putAll(im);
			}
		}
		importMaps = im4src.values();
		map.put("importCommands", importMaps);
		
		// scenarios
		map.put("scenarios", getScenarios());
		map.put("scenarioTexts", Containers.applyToMap(getScenarios(), (s,on) -> s.ruleText));
		
		// settings -- in the PlanDoc
		
		//done
		return map;
	}


	public void run() {
		BusinessContext.setBusiness(this);
		
		// HACK: Add in rows from csv imports (needed before making a BusinessState)
		phase = KPhase.IMPORT;
		for(ImportCommand ic : getImportCommands()) {
			if (ic instanceof CompareCommand) continue; // later
			if (ic.getVarName() != null) {
				continue;
			}
			ic.run2_importRows(this);
		}
		
		run2_removeOffRows();

		// Wot no sampling? USeful for debugging and speed. So this is the default
		int samples = getSettings().getSamples();
		if (samples < 2) {
			setState(new BusinessState(this));
			run2();		
		} else {	
			//
			monteCarloStates = new BusinessState(this);
			for(int i=0; i<samples; i++) {
				// A fresh state 
				setState(new BusinessState(this));
				Log.d("Business", "Sample "+(i+1)+" of "+samples);
				run2();
				
				// add to monteCarlo
				run2_updateMonteCarlo();
			}
			// switch to the stochastic for final outputs
			setState(monteCarloStates);
		}
		
		phase = KPhase.OUTPUT;
	}

	private void setState(BusinessState businessState) {
		this.state = businessState;
	}

	private void run2() {
		phase = KPhase.IMPORT;
		for(ImportCommand ic : getImportCommands()) {
			if (ic instanceof CompareCommand) continue; // later
			ic.run(this);
		}
		
		phase = KPhase.RUN_SIM;
//		ContextAlteringList<Col> cols = new ContextAlteringList<Col>(columns, Cell.CURRENT_COL);			
//		ContextAlteringList<Row> rows = new ContextAlteringList<Row>(getRows(), Cell.CURRENT_ROW);
		List<Row> rows = getRows();
		for(Col col : columns) {
			for(Row row : rows) {					
				Cell cell = new Cell(row, col);
				run3_evaluate(cell);							
			}
		}
		
		for(ImportCommand ic : getImportCommands()) {
			if (ic instanceof CompareCommand) {
				ic.run(this);
			}
		}
	}


	/**
	 * 
	 * @return case & canonicalisation insensitive (all variants included!) name -to-> row-name
	 */
	public Dictionary getRowNames() {
		Dictionary rowNames = new Dictionary();
		for(Row row : getRows()) {
			final String name = row.getName();
			rowNames.add(name, name);
			String rn = StrUtils.toCanonical(name);
			rowNames.add(rn, name);
			rn = rn.replaceAll("[^a-zA-Z0-9]", "");			
			rowNames.add(rn, name);			
		}
		return rowNames;
	}

	/**
	 * Calculate the value for this cell. This will not recalculate (as a result, imports are protected).
	 * <p>
	 * NB: The {@link Row#calculate(Col, Business)} method will update the {@link #state} to hold the value.
	 *  
	 * @param cell
	 * @return 
	 */
	public Numerical run3_evaluate(Cell cell) {		
		// HACK Summary - 1 value only
		if (cell.row.getName().equals("Summary")) {
			return null;
		}
		if (cell.col.index != 1 && cell.row.getParent() != null && cell.row.getParent().getName().equals("Summary")) {
			Numerical v = Business.EMPTY; // no need to recaluclate this if eg its in a sum
			state.set(cell, v);
			return v;
		}
		
		final Numerical beforeSet = state.get(cell);
		Numerical v = beforeSet;
		if (v==EVALUATING) {
			throw new StackOverflowError("This could loop forever: "+cell);
		}
		if (v!=null) {
			// When to tag imports? Done early so tags are there for use in follow-on formula 
//			if (ImportCommand.isImported(v)) {
//				cell.row.tagImport(cell, v);
//			}
			return v;	// NB: this means imports won't get overwritten
		}
		// set flag
		state.set(cell, EVALUATING);

		// calculate!
		try {
			v = cell.row.calculate(cell.col, this);
		} catch(Throwable ex) {
			// allow errors to carry on - note that they propagate
			v = new ErrorNumerical(ex);
		}
		if (v==null) {
			v = Business.EMPTY; // no need to recaluclate this if eg its in a sum
		} else  {
			v.excelRef = GSheetFromMS.cellRef(cell.row, cell.col);
		}
		assert beforeSet==null || beforeSet==EVALUATING || beforeSet==v || v instanceof ErrorNumerical 
				: beforeSet+" vs "+v;		
		// clear evaluating flag
		state.set(cell, v);
		return v;
	}

	private void run2_updateMonteCarlo() {
		phase = KPhase.COLLECT_RESULTS;
		for(Col col : getColumns()) {
			for(Row row : getRows()) {		
				Cell cell = new Cell(row, col);
				Numerical v = getCellValue(cell);
				if (v==null) v = Numerical.NULL;
				UncertainNumerical mc = (UncertainNumerical) monteCarloStates.get(cell);
				// code null as 0 for averaging
				if (mc==null) {
					mc = new UncertainNumerical(new Particles1D(new double[0]), v.getUnit());
					monteCarloStates.set(cell, mc);
				}
				if (mc.getUnit()==null && v.getUnit() != null) {
					mc = new UncertainNumerical(mc.getDist(), v.getUnit());
					monteCarloStates.set(cell, mc);
				}
				// add to the dist
				Particles1D dist = (Particles1D) mc.getDist();
				dist.add(v.doubleValue());
			}
		}
	}

	public Row getRow(String name) {
		// NB: this showed as a minor bottleneck, Jan 2022
		Row row = _row4name.get(name);
		if (row != null) {
			return row;
		}
//		// a switch-row? Done in RowName
//		if (getVars().isSwitchRow(name)) {
//			// find the right match
//			String selected = vars.getActiveRow(name);
//			Row selRow = _row4name.get(selected);
//			return selRow;
//		}
		return null;
	}

	/**
	 * Copy pasta from ImportCommand ?? refactor to share code
	 * @param rowName
	 * @param rowNames 
	 * @return
	 */
	String getRow2(String rowName, Dictionary rowNames) {		
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
				String found = getRow2(rn2, rowNames);
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
	

	/**
	 * 
	 * @param row
	 * @return -1 if not known
	 */
	public int getRowIndex(Row row) {
		return getRows().indexOf(row);
	}

	/**
	 * @deprecated confusing 0 index
	 * @param rowIndex
	 * @param month 0 indexed!
	 * @return
	 */
	public Numerical getCell(int rowIndex, int month) {
		Row row = getRows().get(rowIndex);
		Col col = getColumns().get(month);
		return getCellValue(new Cell(row, col));
	}
	
	private void run2_removeOffRows() {
		for(Row row : _rows.toArray(new Row[0])) {
			if (row.isOn()) continue;
			_rows.remove(row);			
			// remove from _row4name ??
		}
	}

	public List<Row> getRows() {
		// filter off rows here? No that's confusing
		return Collections.unmodifiableList(_rows);
	}
	
	/**
	 * Only use Particles1D here!
	 */
	BusinessState monteCarloStates;
	
	/** NB: this is reset by run() before each evaluation.
	Tests may poke it directly.
	 */
	public BusinessState state ;
	
	/**
	 * Get / evaluate if unset
	 * @param cell
	 * @return
	 */
	public Numerical getCellValue(Cell cell) {	
		Numerical n = state.get(cell);
		if (n==null) {
			n = run3_evaluate(cell);
//			if ( ! Double.isFinite(n.doubleValue())) { // debug
//				n = run3_evaluate(cell);
//			}
		}		
		return n;
	}

	/**
	 * Add to the row(s) rules. 
	 * NB: Row must already exist.
	 * @param rule
	 */
	public void addRule(Rule rule, Collection<Row> rows) {
		if (rule instanceof ScenarioRule) {
			Scenario s = rule.getScenario();
			assert s != null : rule;
			scenarios.put(s, false);
			return;
		}
		// change the selector??
		CellSet sel = rule.getSelector();
		VarSystem vars = Business.get().getVars();
		if (vars.isSwitchRow(sel)) {
//			throw new TodoException();
		}

//		Collection<String> rows = rule.getSelector().getRowNames(null);
		// NB: empty rows can happen for later groups that don't capture their rows.
		for (Row row : rows) {			
//			Row row = getRow(rn);
//			assert row != null : rn;
			row.addRule(rule);			
		}		
	}

//
//	/**
//	 * Replace in row rules. Is this needed??
//	 */
//	public void replaceRule(Rule oldRule, Rule newRule) {
//		Collection<String> rows = newRule.getSelector().getRowNames(null);
//		for (String rn : rows) {			
//			Row row = getRow(rn);
//			assert row != null;
//			int i = row.rules.indexOf(oldRule);
//			if (i != -1) {
//				row.rules.set(i, newRule);
//			}
//		}		
//	}

	
	/**
	 * Ahem, this does not include non-row rules, like "start: Jan 2020"
	 * Or scenario rules
	 * @return
	 */
	public Set<Rule> getAllRules() {
		Set<Rule> rules = new HashSet<Rule>();
		for(Row row : getRows()) {
			List<Rule> rs = row.getRules();
			rules.addAll(rs);
		}
		return rules;
	}

	/**
	Can be null
	*/
	public void addRow(Row row, PlanSheet planSheet) {
		assert _rows.indexOf(row) == -1 : "duplicate row "+row;
		_rows.add(row);
		_row4name.put(row.name, row);
		// change state
		if (state!=null) {
			state.resize(_rows.size(), columns.size());
		}
		addRow2_linkToPlanSheet(row, planSheet);
	}

	public void addRow2_linkToPlanSheet(Row row, PlanSheet planSheet) {
		// plansheet-row tracking
		if (planSheet !=null) {
			String pid = planSheet.getId();
			rows4plansheet.addOnce(pid, row.getName());
			plansheet4row.put(row.getName(), planSheet); // ??what about rows in 2 plansheets?
//			Log.d("Sheets", pid+" <- "+row);
		}
	}
	
	final ListMap<String,String> rows4plansheet = new ListMap(); 
	Map<String, PlanSheet> plansheet4row = new HashMap();

	public ListMap<String, String> getRows4plansheet() {
		return rows4plansheet;
	}
	
	public Dt getTimeStep() {
		return settings.timeStep;
	}

	/**
	 * NB: this list is of course 0-indexed. But Col is 1-indexed, 
	 * so columns[i].index == i + 1
	 */
	private List<Col> columns;
	
	public List<Col> getColumns() {		
		return columns;
	}

	/**
	 * Poke a value into a cell out-with the simulation. 
	 * @param row
	 * @param col
	 * @param value
	 */
	@Deprecated
	public
	void put(Cell cell, Numerical value) {
		state.set(cell, value);
	}

	public String getCSSForCell(Cell cell) {
		StringBuilder sb = new StringBuilder();
		// loop over this + ancestors
		Row arow = cell.row;
		while(arow != null) {
			List<StyleRule> rules = arow.getStyleRules();
			for (StyleRule rule : rules) {			
				if (rule.getSelector() instanceof RowName) {
//					continue; // done at the row level
				}
				if ( ! rule.getSelector().contains(cell, cell)) continue;
				String css = rule.getCSS();
				if (css==null) continue;				
				sb.append(css.trim());
				// make sure it's closed with a ;
				if ( ! css.endsWith(";")) sb.append(";");
			}
			arow = arow.getParent();
		}		
		return sb.toString();
	}
	

	public String getCSSForRow(Row row) {
		StringBuilder sb = new StringBuilder();
		// loop over this + ancestors
		Row arow = row;
		while(arow != null) {
			List<StyleRule> rules = arow.getStyleRules();
			for (StyleRule rule : rules) {			
				if ( ! (rule.getSelector() instanceof RowName)) {
					continue; // cell level
				}
				String css = rule.getCSS();
				sb.append(css);
			}
			arow = arow.getParent();
		}		
		return sb.toString();
	}


	/**
	 * Use during parsing when a sub-row has its parentage set.
	 * This shuffles such sub-rows up to be part of their group.
	 * 
	 * Use-case: define current Staff, then add a new staff member later on in the plan
	 */
	public void reorderRows() {
		ArrayList<Row> rows2 = new ArrayList<Row>();
		for(Row row : _rows) {
			reorderRows2(row, rows2);
		}
		_rows = rows2;	
	}

	private void reorderRows2(Row addMe, ArrayList<Row> rows2) {
		if (rows2.contains(addMe)) return;
		rows2.add(addMe);
		for(Row kid : addMe.getChildren()) {
			reorderRows2(kid, rows2);
		}			
	}

	/**
	 * set by constructor so never null
	 */
	Settings settings;

	private KPhase phase;

	public String title;

	public String getTitle() {
		return title;
	}
	
	public void setSettings(Settings settings) {
		this.settings = settings;
		Dt runTime = settings.getRunTime();
		// NB: we typically get e.g. 23.9999 months
		int months = (int) Math.round(runTime.divide(settings.timeStep));
//		months += 1; // include the end month - done by a hack on end
		setColumns(months);
	}

	public KPhase getPhase() {
		return phase;
	}

	public static enum KPhase {
		IMPORT, RUN_SIM, COLLECT_RESULTS, OUTPUT
	}

	public List<MetaRule> getChartRules() {
		List<MetaRule> crules = new ArrayList<MetaRule>();
		for (Rule rule : getAllRules()) {
			if ( ! (rule instanceof MetaRule)) continue;
			if (((MetaRule)rule).meta.startsWith("plot")) {
				crules.add((MetaRule) rule);
			}
		}
		return crules;
	}

	public static final Business get() {
		return BusinessContext.getBusiness();
	}

	/**
	 * 
	 * @param time
	 * @return Can be a Col outside the range!
	 */
	public Col getColForTime(Time time) {
		TimeSlicer timeSlicer = getSettings().getTimeSlicer();
		if (timeSlicer.getStart().isAfter(time)) {
			return Col.THE_PAST;
		}
		if (timeSlicer.getEnd().isBefore(time)) {
			return Col.THE_INDEFINITE_FUTURE;
		}
		int i = timeSlicer.getBucket(time);
		if (i < 0 || i >= columns.size()) { // paranoia
			return null;
		}
		Col coli = columns.get(i);
		assert coli.index == i + 1;
		return coli;
	}

	public final Settings getSettings() {
		return settings;
	}

	/**
	 * 
	 * @return {parse: {rows, rowtree}}
	 */
	public ArrayMap getParseInfoJson() {
		List<Row> rows = getRows();
		// make a tree of row names
		Tree<String> rowtree = getRowTree();		
		// row rules
		ListMap<String,Map> rulesForRow = new ListMap();
		for (Row row : rows) {
			List<Rule> rules = row.getRules();
			for (Rule rule : rules) {
				rulesForRow.add(row.getName(), 
					new ArrayMap(
							"src", rule.src,
							"comment", rule.getComment()
					)
				);
			}
		}
		// chart rules, in json safe format
		Object crules = WebUtils2.parseJSON(Gson.toJSON(getChartRules()));
		return new ArrayMap("parse", new ArrayMap(
				"rows", Containers.apply(rows, Row::getName), 
				"rowtree", rowtree.toJson2(),
				"rulesForRow", rulesForRow,
				"charts", crules
				));
	}

	public Tree<String> getRowTree() {
		List<Row> rows = getRows();
		Tree<String> rowtree = new Tree();		
		for (Row row : rows) {
			if (row.getParent()!=null) continue;			
			Tree<String> node = new Tree(rowtree, row.getName());
			makeRowTree2(node, row);
		}
		return rowtree;
	}

	/**
	 * recurse
	 * @param node
	 * @param row
	 */
	private void makeRowTree2(Tree<String> node, Row row) {
		List<Row> kids = row.getChildren();
		if (Utils.isEmpty(kids)) return;
		for (Row row2 : kids) {
			Tree<String> node2 = new Tree<>(node, row2.getName());
			makeRowTree2(node2, row2);	
		}		
	}

	/**
	 * @deprecated Use getSettings() instead
	 * @param i
	 */
	@Deprecated
	public void setSamples(int i) {
		getSettings().setSamples(i);
	}

	public void addImportCommand(ImportCommand ic) {
		if (importCommands.contains(ic)) return;
		importCommands.add(ic); 
	}
	

//	public void addExportCommand(ExportCommand ic) {
//		if (exportCommands.contains(ic)) return;
//		exportCommands.add(ic); 
//	}
	
	private List<ImportCommand> importCommands = new ArrayList<>();

//	/**
//	 * Flag to control whether excel formulae are constructed during a run
//	 */
//	public transient boolean isExportToGoogle;
//	
	public Map<Scenario, Boolean> getScenarios() {
		if (scenarios==null) {
			scenarios = new ArrayMap(); // paranoia
		}
		return scenarios;
	}

	public void setScenarios(Map<Scenario, Boolean> map) {
		this.scenarios = map;
	}

	public List<ImportCommand> getImportCommands() {
		return importCommands;
	}

	public void setScenarios(List scs) {
		Map<Scenario, Boolean> bscs = getScenarios();
		// preserve false keys! Just false everything
		for (Object s : bscs.keySet().toArray()) {
			bscs.put((Scenario) s, false);
		}
		// set true
		for (Object s : scs) {
			Scenario scenario = s instanceof Scenario? (Scenario) s : new Scenario((CharSequence) s);
			bscs.put(scenario, true);
		}
		setScenarios(bscs);
	}

	public Scenario getScenario(String rn) {
		Set<Scenario> sc = getScenarios().keySet();
		for (Scenario scenario : sc) {
			if (rn.equals(scenario.name)) return scenario;
		}
		return null;
	}

	public PlanSheet getPlanSheetForRow(Row row) {
		return plansheet4row.get(row.getName());
	}

	transient VarSystem vars;
	
	public VarSystem getVars() {
		if (vars==null) vars = new VarSystem();
		return vars;
	}
}

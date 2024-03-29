package com.winterwell.moneyscript.lang;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.winterwell.moneyscript.lang.cells.CellSet;
import com.winterwell.moneyscript.lang.cells.RowName;
import com.winterwell.moneyscript.lang.cells.RowNameWithFixedVariables;
import com.winterwell.moneyscript.lang.cells.Scenario;
import com.winterwell.moneyscript.lang.cells.SetVariable;
import com.winterwell.moneyscript.lang.num.Formula;
import com.winterwell.moneyscript.lang.num.LangNum;
import com.winterwell.moneyscript.lang.num.Numerical;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.BusinessContext;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Row;
import com.winterwell.moneyscript.output.RuleException;
import com.winterwell.moneyscript.output.VarSystem;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Pair;
import com.winterwell.utils.containers.Pair2;
import com.winterwell.utils.log.Log;

/**
 * Warning: Rules get cached so do NOT edit them after construction
 * @author daniel
 *
 */
public class Rule implements IReset {

	private CellSet selector;
	private String comment;
	
	public String getComment() {
		return comment;
	}
	
	public int getLineNum() {
		return lineNum;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(lineNum, scenario, src);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Rule other = (Rule) obj;
		return lineNum == other.lineNum && Objects.equals(sheetId, other.sheetId) 
				&& Objects.equals(scenario, other.scenario) && Objects.equals(src, other.src);
	}

	/**
	 * @deprecated usually done in constructor
	 * @param selector
	 */
	void setSelector(CellSet selector) {
		this.selector = selector;
	}
	
	public CellSet getSelector() {
		if (selector==null) {
			Log.w("Rule", "null selector? "+this); 
		}
		return selector;
	}
	
	Rule setComment(String comment) {
		if (comment != null) {
			comment = comment.trim();
			if (comment.startsWith("//")) comment = comment.substring(2).trim();
			this.comment = comment;
		}
		return this;
	}
	
	Formula formula;
	public final Formula getFormula() {
		return formula;
	}
		
	/**
	 * How many tabs in? for rule groups
	 */
	int indent;	
	/**
	 * If not null, this rule only applies under this scenario
	 */
	private Scenario scenario;
	
	public Rule(CellSet selector, Formula formula, String src, int indent) {
		super();
		this.selector = selector;
		this.formula = formula;
		this.src = src;
//		if (src!=null && src.contains("leadership")) { // DEBUG hack
//			System.out.println(src);
//		}
		this.indent = indent;
	}
	
	public final String src;
	
	int lineNum;
	
	private String unit;
	String sheetId;
	private String tag;
	/**
	 * NB: transient so we dont share group-level tags via the rule cache
	 */
	private transient String tag2 = "";
	private boolean only;
	
	public String getTag() {
		if (tag != null) {
			return tag;
		}
		if (tag2 != null && ! tag2.isEmpty()) {
			return tag2;
		}		
		return null;
	}
	
	@Override
	public String toString() {
		return src==null? super.toString() : src;
	}
	
	/**
	 *  
	 * @param cell
	 * @return null if the rule has no effect
	 */
	public final Numerical calculate(Cell cell) {
		try {
//			String s = this+" "+cell; // debug
//			if (s.contains("TADG_Display.Donation [Method=PMP]")) {
//				System.out.println(s);
//			}
			BusinessContext.setActiveRule(this);
			// Are we in a scenario - if so, does this rule apply?
			// ?? filter out earlier
			if ( ! isActiveScenario(cell)) {
				return null;
			}
			// Filtered out?
			CellSet sel = getSelector();
			// NB: An optimisation: assume "yes" for RowName, as this is the common setup, and a Rule can't be attached to a non-matching row
			if ( ! (sel instanceof RowName) && ! sel.contains(cell, cell)) {
				return null;
			}
			Numerical v;
			try (Closeable reset = setAnyVariables(cell, sel)) { // e.g. Price [Region=UK]: £5
				v = calculate2_formula(cell);
			}
			if (v==null) {
				return v; // null
			}
			assert v != Business.EVALUATING : cell+" "+this;
			// Should we allow local distributions -- or force all stochastic work to be done by global samples?
			// Local distros have the problem that they might be sampled twice with different results!
			// e.g. Sales: 1 +- 1		Revenue: Sales * £10		CostOfSales: Sales * £2 	<-- Sales must return the same answers
			// TODO allow UncertainNumerical to fix its value when it is sampled - but preserve the distro info.
			// For now: convert to number
			if (v instanceof UncertainNumerical) {
				Numerical vFixed = ((UncertainNumerical) v).sample();
				v = vFixed;
			}
			// Allow NaN or infinity (so the user can debug their formula)
			if ( ! Double.isFinite(v.doubleValue())) {
				Log.w("Rule", "not finite "+cell+" "+this);
//				throw new IllegalArgumentException(v+" for "+this+" on "+cell);
			}
			// allow the script to override and set what the unit is, e.g. "Margin (%): Profit / Income" 
			if (unit != null) {
				v.setUnit(unit);
			}
			calculate2_tag(cell, v);
			return v;
		} catch(Throwable ex) {
			throw new RuleException(ex+" Cell "+cell+" Rule "+this, ex);
		}
	}

	static final Closeable NOOP = new NoOp();
	
	/**
	 * // e.g. Price [Region=UK]: £5 => Region=UK during this rule
	 * @param cell 
	 * @param selector2
	 * @return
	 */
	private Closeable setAnyVariables(Cell cell, CellSet selector2) {
		Collection<SetVariable> vals = selector2.getVars(cell);
		if (vals.isEmpty()) {
			return NOOP;
		}
		Business biz = Business.get();
		VarSystem vars = biz.getVars();
		List<Pair<String>> resets = new ArrayList<>();
		for (SetVariable sv : vals) {
			String prev = vars.setRow4Name(sv.var,  sv.value);
			resets.add(new Pair(sv.var, prev));
		}
		return new ResetVars(vars, resets);
	}

	private void calculate2_tag(Cell cell, Numerical v) {
		String _tag = getTag();
		if (_tag==null) return;
		// Hack: special tag?
		_tag = LangNum.resolveTag(_tag, cell);
		v.setTag(_tag);		
	}

	public boolean isActiveScenario(Cell context) {
		if (scenario==null) return true;
		Map<Scenario, Boolean> scs = context.getBusiness().getScenarios();				
		boolean on = Utils.yes(scs.get(scenario));
		return on;
	}

	/**
	 * Called after filters have been applied
	 * @param b
	 * @return
	 */
	protected Numerical calculate2_formula(Cell b) {
		if (formula==null) return null;
		Numerical v = formula.calculate(b);
		return v;
	}

	void setScenario(Scenario byScenario) {
		// scenario can only be set once, to protect against confusing setups
//		assert this.scenario==null || this.scenario.equiv(byScenario) : "scenario conflict: "+this.scenario+" vs "+byScenario+" in "+this;
		// but cached rules were clashing on this		
		this.scenario = byScenario;
		// HACK - track the text for user info
		if (scenario!=null) {
			if (src!=null && ! scenario.ruleText.contains(src)) { // cache => dupe check 
				scenario.ruleText += src;
			}
		}
	}

	public Scenario getScenario() {
		return scenario;
	}

	@Override
	public void reset() {
		// clear the scenario. Why? cos a line of script might move between scenarios in an edit, but our parsing cache wouldn't know that.
		scenario = null;
	}

	void setUnit(String unit) {
		this.unit = unit;
	}

	/**
	 * e.g. "+ 10%"
	 * @return
	 */
	public boolean isStacked() {
		return formula !=null && formula.isStacked();
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	/**
	 * NB: This is transient, so not shared via cache
	 * @param tag2
	 */
	public void setTagFromParent(String tag2) {
		this.tag2 = tag2;
	}

	public void tagImport(Cell cell, Numerical v) {
		if (getTag()==null) return;
		BusinessContext.setActiveRule(this);
		// Are we in a scenario - if so, does this rule apply?
		// ?? filter out earlier
		if ( ! isActiveScenario(cell)) {
			return;
		}
		// Filtered out?
		if ( ! getSelector().contains(cell, cell)) {
			return;
		}
		// tag it
		calculate2_tag(cell, v);
	}

	public void setOnly(boolean isOnly) {
		only = isOnly;
	}

	public final boolean isOnly() {
		return only;
	}


}

final class NoOp implements Closeable {
	@Override
	public void close() throws IOException {	
	}
}

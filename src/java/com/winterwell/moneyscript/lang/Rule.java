package com.winterwell.moneyscript.lang;

import java.util.Map;

import com.winterwell.moneyscript.lang.cells.CellSet;
import com.winterwell.moneyscript.lang.cells.Scenario;
import com.winterwell.moneyscript.lang.num.Formula;
import com.winterwell.moneyscript.lang.num.Numerical;
import com.winterwell.moneyscript.output.BusinessContext;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.RuleException;
import com.winterwell.utils.Utils;
import com.winterwell.utils.log.Log;


public class Rule {

	private final CellSet selector;
	private String comment;
	
	public String getComment() {
		return comment;
	}
	
	public CellSet getSelector() {
		if (selector==null) {
			Log.w("Rule", "null selector? "+this); 
		}
		return selector;
	}
	
	public Rule setComment(String comment) {
		if (comment != null) {
			comment = comment.trim();
			if (comment.startsWith("//")) comment = comment.substring(2).trim();
			this.comment = comment;
		}
		return this;
	}
	
	public Formula formula;
	/**
	 * How many tabs in? for rule groups
	 */
	int indent;	
	/**
	 * If not null, this rule only applies under this scenario
	 */
	Scenario scenario;
	
	public Rule(CellSet selector, Formula formula, String src, int indent) {
		super();
		this.selector = selector;
		this.formula = formula;
		this.src = src;
		this.indent = indent;
	}
	
	public final String src;
	
	int lineNum;
	
	@Override
	public String toString() {
		return src==null? super.toString() : src;
	}
	
	/**
	 *  
	 * @param b
	 * @return null if the rule has no effect
	 */
	public Numerical calculate(Cell b) {
		try {
			BusinessContext.setActiveRule(this);
			// Are we in a scenario - if so, does this rule apply?
			if (scenario!=null) {
				Map<Scenario, Boolean> scs = b.getBusiness().getScenarios();				
				if ( ! Utils.yes(scs.get(scenario))) {
					return null;
				} else { // NB: allow for breakpoints here
					Object debugActiveScenario = scs;
					assert debugActiveScenario != null;
				}
			}		
			// Filtered out?
			if ( ! getSelector().contains(b, b)) {
				return null;
			}
			if (formula==null) return null;
			Numerical v = formula.calculate(b);
			// Issue: conditional probs require global samples - working with local marginals is wrong
			assert ! (v instanceof UncertainNumerical);
	//			Double s = ((UncertainNumerical) v).getDist().sample();
	//			v = new Numerical(s, v.getUnit());
	//		}
			return v;
		} catch(Throwable ex) {
			throw new RuleException(ex+" Cell "+b+" Rule "+this, ex);
		}
	}

	public void setScenario(Scenario byScenario) {
		// scenario can only be set once, to protect against confusing setups
		assert this.scenario==null || this.scenario.equiv(byScenario) : this;
		this.scenario = byScenario;
	}

	public Scenario getScenario() {
		return scenario;
	}


}

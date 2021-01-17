package com.winterwell.moneyscript.lang;

import com.winterwell.moneyscript.lang.cells.RowName;
import com.winterwell.moneyscript.lang.cells.Scenario;

public class ScenarioRule extends GroupRule {

	public ScenarioRule(Scenario scenario, int indent) {
		super(new RowName("scenario "+scenario), indent);
		setScenario(scenario);
	}

	@Override
	public void reset() {
		// preserve scenario, but clear its text
		Scenario s = getScenario();
		super.reset();
		this.setScenario(s);
		s.ruleText = "";
	}
}

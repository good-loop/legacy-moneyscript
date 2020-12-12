package com.winterwell.moneyscript.output;

import java.util.Collection;

import com.winterwell.moneyscript.lang.Rule;
import com.winterwell.moneyscript.lang.cells.Scenario;
import com.winterwell.utils.Environment;
import com.winterwell.utils.Key;

/**
 * merge with cell??
 * This is "where is the program run focus?"
 * @author daniel
 *
 */
public final class BusinessContext {

	private static final Key<Business> BUSINESS = new Key<Business>("business");
	private static final Key<Rule> ACTIVE_RULE = new Key<Rule>("rule");
	private static final Key<Collection<Scenario>> SCENARIO = new Key<>("scenario");
	
	public static Business getBusiness() {
		Environment env = Environment.get();
		return env.get(BUSINESS);
	}
	
	public static void setBusiness(Business business) {
		Environment env = Environment.get();
		env.put(BUSINESS, business);
	}
	
	
	public static void setActiveRule(Rule rule) {
		Environment env = Environment.get();
		env.put(ACTIVE_RULE, rule);
	}	

}

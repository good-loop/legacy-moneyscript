//package com.winterwell.moneyscript.lang.cells;
//
//import java.util.Collection;
//import java.util.Collections;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//
//import com.winterwell.moneyscript.output.Business;
//import com.winterwell.moneyscript.output.BusinessContext;
//import com.winterwell.moneyscript.output.Cell;
//import com.winterwell.moneyscript.output.Col;
//import com.winterwell.moneyscript.output.Row;
//
//public final class ScenarioName extends CellSet {
//	public final Scenario scenario;
//	
//	public ScenarioName(Scenario scenario) {
//		super(scenario.name);
//		this.scenario = scenario;
//	}
//	
//	@Override
//	public Set<String> getRowNames(Cell focus) {
//		return Collections.emptySet();
//	}
//	
//	@Override
//	public boolean contains(Cell cell, Cell context) {
//		return false;
//	}
//
//	@Override
//	public Collection<Cell> getCells(Cell bc, boolean wide) {
//		return Collections.emptyList();
//	}
//}
//

package com.winterwell.moneyscript.output;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.winterwell.moneyscript.lang.cells.CellSet;
import com.winterwell.moneyscript.lang.cells.RowName;
import com.winterwell.moneyscript.lang.cells.SetVariable;
import com.winterwell.moneyscript.lang.num.Formula;
import com.winterwell.moneyscript.lang.num.VariableDistributionFormula;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.containers.ArraySet;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.containers.ListMap;
import com.winterwell.utils.containers.Tree;

/**
 * Manage M$ variable logic.
 * 
 * Concepts
 * 
 * switch-row-values, e.g. `Price [Region=UK]: £2`
 * TODO or `Price.UK: £2`
 * 
 * switch-row-references, e.g. "Price" in `Tax: 20% * Price`
 * 
 * sum-over loops `[...]` and variables e.g. "Region" in `[Region in RegionMix: Region.Sales]`
 * 
 * Row properties, e.g. "Region.Sales" can resolve to "UK.Sales"
 * 
 * Expanding rows, where a formula references a switch-row, and so is expanded to a few variants
 * e.g. `Revenue: Price * Sales` expanding to several output rows
 * 	`Revenue [Region=UK]: Price [Region=UK] * UK.Sales`
 * 	`Revenue [Region=US]: Price [Region=US] * US.Sales`
 * 
 * @author daniel
 *
 */
public class VarSystem {

	/**
	 * var name to "enum" of values, e.g. Region:[UK,US,EU]
	 */
	ListMap<String,String> var2values = new ListMap();


	/**
	 * fn row name to relevant vars, e.g. Price:[Region]
	 * 
	 * This allows fn-rows to be expanded into "plain" rows
	 */
	ListMap<String,String> switchRow2vars = new ListMap();

	private void addSetVariable(SetVariable v) {
		var2values.addOnce(v.var, v.value);
	}

	public List<String> getVariableValues(String var) {
		List<String> vals = var2values.get(var);
		return vals;
	}

	public void addSwitchRow(String baseName, Collection<SetVariable> vars) {
		for(SetVariable v : vars) {
			switchRow2vars.addOnce(baseName, v.var);
			addSetVariable(v);
		}
	}

	public static String getBaseName(String name) {
		String baseName = name.substring(0, name.indexOf("[")).trim();
		return baseName;
	}

	/**
	 * 
	 * @param sel
	 * @return true for e.g. "Region" if we have "Price [Region=US]: $2"
	 */
	public boolean isSwitchRow(CellSet sel) {
		if (sel instanceof RowName) {
			String rn = ((RowName) sel).getRowName();
			boolean issr = switchRow2vars.containsKey(rn);
			// cache this??
			return issr;
		}
		return false;
	}

	public static List<String> getVarNames(Formula f) {
		Tree<Formula> tree = f.asTree();
		List<Formula> fs = tree.flattenToValues();
		List<VariableDistributionFormula> varfs = Containers.filterByClass(fs, VariableDistributionFormula.class);
		List<String> varNames = Containers.apply(varfs, varf -> varf.getVar());
		return varNames;
	}

	public Set<String> expandRowNames(Set<String> rowNames, RowName switchRowName) {
		assert isSwitchRow(switchRowName);
		String switchName = switchRowName.getRowName();
		ArraySet<String> expanded = new ArraySet();
		for (String rn : rowNames) {
			List<String> vs = switchRow2vars.get(switchName);
			if (vs==null) {
				expanded.add(rn);
				continue;
			}
			if (vs.size() > 1) throw new TodoException(rowNames+" "+vs);
			for (String var : vs) {
				List<String> vals = var2values.get(var);
				for (String val : vals) {
					expanded.add(rn+" ["+var+"="+val+"]");
				}
			}
		}
		return expanded;
	}
	
}

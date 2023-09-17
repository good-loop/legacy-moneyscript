package com.winterwell.moneyscript.output;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.winterwell.moneyscript.lang.cells.CellSet;
import com.winterwell.moneyscript.lang.cells.RowName;
import com.winterwell.moneyscript.lang.cells.SetVariable;
import com.winterwell.moneyscript.lang.num.BasicFormula;
import com.winterwell.moneyscript.lang.num.Formula;
import com.winterwell.moneyscript.lang.num.VariableDistributionFormula;
import com.winterwell.utils.StrUtils;
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
 * sum-over-group loops `[...]` and variables e.g. 
 * 	"Region" in `[Region in RegionMix: Region.Sales]`
 * 
 * Row properties, e.g. "Region.Sales" can resolve to "UK.Sales"
 * 
 * Expanding rows, where a formula references a switch-row, and so is expanded to a few variants
 * e.g. `Revenue: Price * Sales` expanding to several output rows
 * 	`Revenue [Region=UK]: Price [Region=UK] * UK.Sales`
 * 	`Revenue [Region=US]: Price [Region=US] * US.Sales`
 * 
 * 
 * TODO refactor for speed:
 * After parsing, do a batch wiring-up stage where:
 * Rules are copied so they only refer to one row.
 * RowVar uses less look-up and isSwitchRow() testing 
 * RowName holds Row to avoid Business.getRow() look-ups
 * 
 * @testedby VarSystemTest
 * @author daniel
 *
 */
public final class VarSystem {

	/**
	 * fn row name to relevant vars, e.g. Price:[Region]
	 * 
	 * This allows fn-rows to be expanded into "plain" rows
	 */
	ListMap<String,RowVar> switchRow2vars = new ListMap();

	Map<String,RowVar> name2var = new HashMap();
	
	private void addSetVariable(SetVariable v) {
		RowVar var = getVar(v.var);
		var.addValue(v.value);
	}

	private RowVar getVar(String var) {
		RowVar v = name2var.get(var);
		if (v==null) {
			v = new RowVar(var);
			name2var.put(var, v);
		}
		return v;
	}

	public List<String> getVariableValues(String var) {
		return getVar(var).values;
	}

	public void addSwitchRow(String baseName, Collection<SetVariable> vars) {
		for(SetVariable v : vars) {
			switchRow2vars.addOnce(baseName, getVar(v.var));
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
			boolean issr = isSwitchRow(rn);
			// cache this??
			return issr;
		}
		return false;
	}
	

	public boolean isSwitchRow(String rowName) {
		boolean issr = switchRow2vars.containsKey(rowName);
		return issr;
	}
	
	public List<RowVar> getVars(String rowName) {
		return switchRow2vars.get(rowName);
	}


	public static List<String> getVarNames(Formula f) {
		Tree<Formula> tree = f.asTree();
		List<Formula> fs = tree.flattenToValues();
		List<VariableDistributionFormula> varfs = Containers.filterByClass(fs, 
				VariableDistributionFormula.class);
		List<String> varNames = Containers.apply(varfs, varf -> varf.getVar());
		return varNames;
	}

	public Set<String> expandRowNames(Set<String> rowNames, List<RowVar> refs) {
		if (refs.size() > 1) throw new TodoException(rowNames+" "+refs);
		ArraySet<String> expanded = new ArraySet();
		for (String rn : rowNames) {
			Collection<SetVariable> vars = new ArrayList();
			for(RowVar ref : refs) {
				for (String val : ref.values) {
					expanded.add(rn+" ["+ref.name+"="+val+"]");
					vars.add(new SetVariable(ref.name, val));
				}
			}
			// these are now switch-rows too
			addSwitchRow(rn, vars);
		}
		return expanded;
	}

	public String getActiveRow(String name) {
		List<RowVar> vars = switchRow2vars.get(name);
		if (vars==null) return null;
		String aname = name;
		// HACK (and fragile to spaces and ordering!)
		for (RowVar v : vars) {
			if (v.value==null) return null;
			aname = aname+" ["+v.name+"="+v.value+"]";
		}
		assert isSwitchRow(name) : name;
		return aname;
	}

	/**
	 * Set a variable to point to a row e.g. Region=UK
	 * @param name
	 * @param row Can be null
	 * @return prev setting (reset this to manage nested scope properly)
	 */
	public String setRow4Name(String var, String val) {
		assert var != null;
		String old = getVar(var).setValue(val);
		return old;
	}
	

	@Override
	public String toString() {
		return "VarSystem [switchRow2vars=" + switchRow2vars + ", name2var=" + name2var + "]";
	}

	public String getActiveName(String rowName) {
		String[] bits = rowName.split("\\.");
		String[] modBits = new String[bits.length];
		for (int i = 0; i < bits.length; i++) {
			String bi = bits[i];
			RowVar vari = name2var.get(bi);
			modBits[i] = vari==null? bi : vari.value;
		}
		return StrUtils.join(modBits, '.');
	}

	/**
	 * e.g. "Price" returns Var[Region=US|UK]
	 * @param f
	 * @return
	 */
	public List<RowVar> getVarRefs(Formula f) {
		List<Formula> fs = f.asTree().flattenToValues();
		List<BasicFormula> basics = Containers.filterByClass(fs, BasicFormula.class);
		List<RowVar> refs = new ArrayList();
		for (BasicFormula bf : basics) {
			CellSet sel = bf.getCellSetSelector();
			if (sel instanceof RowName) {
				String rn = ((RowName) sel).getRowName();
				if ( ! isSwitchRow(rn)) continue;
				List<RowVar> vars = switchRow2vars.get(rn);
				if (vars!=null) {
					refs.addAll(vars);
				}
			}
		}
		return refs;
	}

	
	
}

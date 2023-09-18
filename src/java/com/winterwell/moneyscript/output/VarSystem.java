package com.winterwell.moneyscript.output;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.winterwell.moneyscript.lang.cells.CellSet;
import com.winterwell.moneyscript.lang.cells.LangCellSet;
import com.winterwell.moneyscript.lang.cells.RowName;
import com.winterwell.moneyscript.lang.cells.RowNameWithFixedVariables;
import com.winterwell.moneyscript.lang.cells.SetVariable;
import com.winterwell.moneyscript.lang.num.BasicFormula;
import com.winterwell.moneyscript.lang.num.Formula;
import com.winterwell.moneyscript.lang.num.VariableDistributionFormula;
import com.winterwell.nlp.simpleparser.ParseResult;
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
 * TODO remove this and just use switch-rows??
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

	/**
	 * Both loop variables and switch vars
	 */
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

	/**
	 * Repeat eequivalent calls have no effect
	 * @param baseName
	 * @param vars
	 */
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

	public Set<String> expandRowNames(Collection<String> rowNames, List<RowVar> refs) {
		// What if the rowNames have set variables themsleves??
		if (refs.size() > 1) {
			// consistent order
			ArrayList list = new ArrayList(refs);
			Collections.sort(refs);
			refs = list;
		}
		// cross-product of all combinations for refs
		List<List<SetVariable>> allOptions = expandRowNames2(refs, 0);
		// build an expanded set of row-names
		ArraySet<String> expanded = new ArraySet();
		LangCellSet lcs = new LangCellSet();
		for (String rn : rowNames) {
			// just the new ones - respect any SetVariables in the row name
			String baseName = rn;
			Collection<SetVariable> rnVars = Collections.EMPTY_LIST;
			ParseResult<RowNameWithFixedVariables> p = lcs.rowNameWithFixedVariable.parse(rn);
			if (p != null) {
				RowNameWithFixedVariables rnv = p.getX();
				rnVars = rnv.getVars(null);
				baseName = rnv.getBaseName();
			}
			List<String> rnVarNames = Containers.apply(rnVars, SetVariable::getVar);

			for(List<SetVariable> setvs : allOptions) {
				List<SetVariable> setvs2 = new ArrayList(rnVars);
				List<SetVariable> newSVs = Containers.filter(setvs, setv -> ! rnVarNames.contains(setv.var));
				setvs2.addAll(newSVs);
				// make the augmented row-name
				StringBuilder sb = new StringBuilder(baseName);
				sb.append(" ");
				sb.append(setvs2); // HACK: does "["+vals,+"]"
				String rn2 = sb.toString();
				expanded.add(rn2);	// this is a set, so repeats dont matter
				// these are now switch-rows too
				addSwitchRow(rn, setvs);
			}
		}
		return expanded;
	}

	private List<List<SetVariable>> expandRowNames2(List<RowVar> refs, int i) {
		List<List<SetVariable>> options = new ArrayList();
		RowVar ref = refs.get(i);
		for (String val : ref.values) {
			options.add(Collections.singletonList(new SetVariable(ref.name, val)));
		}
		i++;
		if (i == refs.size()) {
			return options;
		}
		List<List<SetVariable>> subOpts = expandRowNames2(refs, i);
		List<List<SetVariable>> allOptions = new ArrayList();
		for (List<SetVariable> opt : options) {
			for (List<SetVariable> subOpt : subOpts) {
				ArrayList combo = new ArrayList(opt);
				combo.addAll(subOpt);
				allOptions.add(combo);
			}
		}
		return allOptions;
	}

	/**
	 * Resolve switch rows
	 * @param name e.g. "Price"
	 * @return e.g. "Price [Region=UK]"
	 */
	public String getActiveRow(String name) {
		assert isSwitchRow(name) : name;
		List<RowVar> vars = switchRow2vars.get(name);
		if (vars==null) return null;
		StringBuilder aname = new StringBuilder(name);
		aname.append(" [");
		// HACK (and fragile to spaces and ordering!)
		for (RowVar v : vars) {
			if (v.value==null) return null;
			aname.append(v.name+"="+v.value);
			aname.append(", ");
		}
		StrUtils.pop(aname, 2);
		aname.append("]");
		return aname.toString();
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
	
	/**
	 * Vars with values
	 * @return
	 */
	public List<RowVar> getCurrentSetVars() {
		Collection<RowVar> allVars = name2var.values();
		List<RowVar> setVars = Containers.filter(allVars, v -> v.value != null);
		return setVars;
	}

	@Override
	public String toString() {
		return "VarSystem [switchRow2vars=" + switchRow2vars + ", name2var=" + name2var + "]";
	}

	/**
	 * Resolve loop-variables and switch-values
	 * @param rowName e.g. "Product.Price"
	 * @return e.g. "MyProduct.Price [Region=UK]"
	 */
	public String getActiveName(String rowName) {
		String[] bits = rowName.split("\\.");
//		String[] modBits = new String[bits.length];
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < bits.length; i++) {
			String bi = bits[i];
			String mbi = bi;
			// loop variable?
			RowVar vari = name2var.get(bi);
			if (vari!=null) {
				mbi = vari.value;
			}
			if (i != 0) sb.append('.');
			sb.append(mbi);
			// switch row?
			if (isSwitchRow(sb.toString())) { // add a set variable to the row name??
				String arow = getActiveRow(sb.toString());
				if (arow!=null) {
					sb = new StringBuilder(arow);
				}
			}
//			modBits[i] = mbi;
		}
		return sb.toString(); // StrUtils.join(modBits, '.');
	}

	/**
	 * e.g. "Price" might return Var[Region=US|UK]
	 * @param f
	 * @return
	 */
	public List<RowVar> getVarRefs(Formula formula) {
		int x = 0;
		Tree<List> newTree = formula.asTree().apply(f -> {
			if (f instanceof VariableDistributionFormula) {
				// don't count vars that get summed over - they're not exposed
				String var = ((VariableDistributionFormula) f).getVar();
				return Collections.singletonList(var); // HACK String => exclude this
			}
			if ( ! (f instanceof BasicFormula)) {
				return Collections.EMPTY_LIST;
			}
			CellSet sel = ((BasicFormula) f).getCellSetSelector();
			if (sel instanceof RowName) {
				String rn = ((RowName) sel).getRowName();
				if ( ! isSwitchRow(rn)) return Collections.EMPTY_LIST;
				List<RowVar> vars = switchRow2vars.get(rn);
				if (vars!=null) {
					return vars; //refs.addAll(vars);
				}
			}
			return Collections.EMPTY_LIST;			
		});
//		List<Formula> fs = f.asTree().flattenToValues();
//		List<BasicFormula> basics = Containers.filterByClass(fs, BasicFormula.class);
//		List<RowVar> refs = new ArrayList();
//		for (BasicFormula bf : basics) {
//			CellSet sel = bf.getCellSetSelector();
//			if (sel instanceof RowName) {
//				String rn = ((RowName) sel).getRowName();
//				if ( ! isSwitchRow(rn)) continue;
//				List<RowVar> vars = switchRow2vars.get(rn);
//				if (vars!=null) {
//					refs.addAll(vars);
//				}
//			}
//		}
		List vals = Containers.flatten(newTree.flattenToValues());
		if (vals.isEmpty()) return vals; // shortcut
		List<RowVar> rvs = Containers.filterByClass(vals, RowVar.class);
		List<String> excludes = Containers.filterByClass(vals, String.class);
		List<RowVar> rvs2 = Containers.filter(rvs, rv -> ! excludes.contains(rv.name));
		ArraySet<RowVar> rvs3 = new ArraySet(rvs2);
		return new ArrayList(rvs3);
	}

	
	
}

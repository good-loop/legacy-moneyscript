package com.winterwell.moneyscript.output;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.winterwell.moneyscript.lang.DummyRule;
import com.winterwell.moneyscript.lang.GroupRule;
import com.winterwell.moneyscript.lang.ImportRowCommand;
import com.winterwell.moneyscript.lang.MetaRule;
import com.winterwell.moneyscript.lang.Rule;
import com.winterwell.moneyscript.lang.StyleRule;
import com.winterwell.moneyscript.lang.UncertainNumerical;
import com.winterwell.moneyscript.lang.cells.CellSet;
import com.winterwell.moneyscript.lang.num.Numerical;
import com.winterwell.utils.IFilter;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.containers.ITree;
import com.winterwell.utils.time.Time;

/**
 * Each line of the plan will produce a Row in the spreadsheet??
 * 
 * Lifecycle: Made late in the parse by Lang.parse()
 * 
 * @author daniel
 *
 */
public final class Row 
implements ITree // NB: we don't use Row ITree anywhere (yet) 
{

	String name;
	
	public Row getParent() {
		return parent;
	}
	
	public Row(String name) {
		this.name = name.trim();
		// HACK - avoid annual totals for Balance, Head Count, etc
		// ??How might we make this a setting in the script??
		// maybe "Total: annual except(Balance)"
		String cname = StrUtils.toCanonical(name).replace(" ","");
		if (Arrays.asList("balance","cashatbank", "cash").contains(cname) || cname.contains("count")) {
			noTotal = true;
		}
	}


	@Override
	public String toString() {
		return name;
	}

	public List<Rule> getRules() {
		if (parent!=null) {					
			ArrayList<Rule> rules2 = new ArrayList<Rule>(rules);
			List<Rule> rs = parent.getRules();
			for (Rule rule : rs) {
				if (rule instanceof GroupRule) continue;
				rules2.add(rule);
			}
			return rules2;
		}
		return rules;
	}

	final List<Rule> rules = new ArrayList<Rule>();
	private int index = -1;
	private List<Row> kids = new ArrayList<Row>();
	private Row parent;
	private List<StyleRule> stylers = new ArrayList<StyleRule>();
	/**
	 * If true don't include a year-end total for this row
	 * e.g. Balance
	 */
	private boolean noTotal;

	/**
	 * Process the cell -- except group cells
	 * @param col
	 * @param b
	 * @return value or null for a group cell
	 */
	public Numerical calculate(Col col, Business b) {	
//		if ("Kate Ho".equals(getName())) {
//			assert true;
//		}
		Cell cell = new Cell(this, col);
		// special case: group rows
		if (isGroup()) {
			// ??But you can also use a group for organisation, to contain a set of child rows
			return getGroupValue(col, cell);
		}
		List<Rule> rs = getRules();		
		if (rs.isEmpty()) return null;
		Numerical v = null;
		// Last rule wins in a straight calculation (some rules are modifiers)
		for (Rule r : rs) {
//			if (r instanceof ImportRowCommand) {
//				System.out.println("debug");
//			}
			if (r instanceof MetaRule) continue;
			if (r instanceof StyleRule) continue;
			if (r instanceof DummyRule) continue;
			if (r instanceof GroupRule) continue;			
			assert r != null : rs;
			// NB: scenario on/off is done inside calculate
			Numerical v2 = r.calculate(cell);
			if (v2==null) {
				continue; // e.g. rule not active yet
			}
			if (Numerical.isZero(v2)) {
				// ?? vs v
			}
			// comment = rule name
			if (v != v2) {
				v2.comment = StrUtils.space(v==null? null : v.comment, r.src);
			}
			v = v2;
			// sum the rules?
			// No - makes badness from A: £1, A: * 150% = £1 + £1*1.5
//			cv = cv.plus(v);
			// and set as we go so rules such as "start" can already reference this value
			b.state.set(cell, v);
		}
		return v;
	}



	public String getName() {
		return name;
	}



	public Collection<Cell> getCells() {
		Business b = Business.get();
		List<Col> cols = b.getColumns();
		List<Cell> list = new ArrayList<Cell>(cols.size());
		for (Col col : cols) {
			list.add(new Cell(this, col));
		}
		return list;
	}



	public int getIndex() {
		if (index != -1) return index;
		Business b = Business.get();
		index = b.getRowIndex(this);
		return index;
	}



	public boolean isOn() {
		// is there an off rule?
		Cell cell = new Cell(this, new Col(1));
		for(Rule r : rules) {
			if (r instanceof MetaRule) {
				String meta = ((MetaRule) r).meta;
				if ( ! "off".equals(meta)) continue;				
				boolean applies = r.getSelector().contains(cell, cell);
				if ( ! applies) continue;				
				return false;
			}
		}
		return true;
	}



	public List<Row> getChildren() {
		return kids;
	}



	public void setParent(Row group) {
		if (parent==group) return;
		assert parent == null;
		parent = group;
		assert ! group.kids.contains(this) : this;
		group.kids.add(this);
		// NB: scenario is done at the rule level (not the row level)
	}



	public boolean isGroup() {
		return kids.size() != 0;
	}

	public double[] getValues() {
		Business b = Business.get();
		List<Cell> cells = Containers.getList(getCells());
		double[] vs = new double[cells.size()];
		for (int i = 0; i < vs.length; i++) {
			Cell c = cells.get(i);
			Numerical v = b.getCellValue(c); 
			vs[i] = (v==null || v==Business.EVALUATING)? 0 : v.doubleValue();
		}
		return vs;
	}

	public Numerical getGroupValue(Col col, Cell b) {
		assert isGroup() : this;
		// HACK na?
		List<Rule> _rules = getRules();
		for (Rule rule : _rules) {
			if (rule instanceof GroupRule && ((GroupRule)rule).isNA()) {
				Numerical n = Business.EMPTY; // HACK 0 isnt skip, but it should do for most cases
				return n;
			}
		}
		// sum over the kids
		List<Row> kids = getChildren();
		
		// sample from distributions? (could we preserve in some cases instead?? But thats messy)
		Business biz = Business.get();
		Numerical dummy = biz.getCellValue(new Cell(kids.get(0), col));
		if (dummy instanceof UncertainNumerical) {
			return getGroupValue2_dist(col, biz);
		}
		
		Numerical sum = new Numerical(0);
		for (Row kid : kids) {
			Numerical v = biz.getCellValue(new Cell(kid, col));			
			assert ! (v instanceof UncertainNumerical) : this; // Sampled above
			if (Numerical.isZero(v)) {
				continue;
			}
			String newComment = StrUtils.joinWithSkip(" + ", sum.comment, 
					kid.getName()+"("+v+")");
			sum = sum.plus(v);
			// what went into the sum?
			sum.comment = newComment;
		}		
		return sum;
	}

	private Numerical getGroupValue2_dist(Col col, Business b) {
		List<Row> kids = getChildren();
		UncertainNumerical dummy = (UncertainNumerical) b.getCellValue(new Cell(kids.get(0), col));
		Particles1D dps = (Particles1D) dummy.getDist();		
		Particles1D ps = new Particles1D(new double[dps.pts.length]);
		String unit = null;
		for (Row kid : kids) {
			UncertainNumerical v = (UncertainNumerical) b.getCellValue(new Cell(kid, col));
			if (v==null) continue;
			Particles1D dist = (Particles1D) ((UncertainNumerical) v).getDist();
			assert ps.pts.length == dist.pts.length;			
			for(int i=0; i<dist.pts.length; i++) {
				ps.pts[i] += dist.pts[i];
			}
			if (unit == null) unit = v.getUnit(); 
		}
		return new UncertainNumerical(ps, unit);
	}

	public List<StyleRule> getStyleRules() {		
		return stylers;
	}

	public void addRule(Rule rule) {
		if (rule instanceof StyleRule) {
			stylers.add((StyleRule) rule);
			sortRules(stylers);
			return;
		}		
		rules.add(rule);
		sortRules(rules);
	}

	/**
	 * sort rules: rules with a filter beat those without, regardless of definition order.
	 * Otherwise it's last-in wins
	 * @param _rules
	 */
	private void sortRules(List<? extends Rule> _rules) {
		Collections.sort(_rules, new Comparator<Rule>() {
			@Override
			public int compare(Rule a, Rule b) {
				CellSet as = a.getSelector();
				CellSet bs = b.getSelector();
				return as.compareTo(bs);
			}			
		});
	}

	public List<MetaRule> getMetaRules() {
		return Containers.filter(rules, new IFilter(){
			public boolean accept(Object x) {return x instanceof MetaRule;}
		});
	}

	public List<Map> getValuesJSON(boolean yearTotals) {
//		if (name.contains("Grant")) {	// debug
//			System.out.println(this);
//		}
		List<Cell> cells = Containers.getList(getCells());
		Business b = Business.get();
		List<Map> list = new ArrayList<Map>();
		for (int i = 0; i < cells.size(); i++) {
			Cell c = cells.get(i);
			Numerical v = b.getCellValue(c);
			// cell json
			ArrayMap map = getValuesJSON2_cell(b, c, v);
			list.add(map);	
			
			// year total?
			if ( ! yearTotals) continue;
			Time t = c.getColumn().getTime();
			if (t.getMonth()!=12) {
				continue;
			}
			// ...avoid for e.g. balance
			if (noTotal) {
				list.add(new ArrayMap());
				continue;
			}
			// sum the year
			Numerical yearSum = new Numerical(0);
//			double delta = 0;
			boolean hasDelta = false;
			for (int j=Math.max(0, i-11); j<=i; j++) {
				Cell cj = cells.get(j);
				Numerical vj = b.getCellValue(cj);
				yearSum = yearSum.plus(vj);
				if (vj.getDelta()!=null) {
//					delta += vj.getDelta();
					hasDelta = true;
				}
			}
			yearSum.comment = "total for year "+t.getYear();
			if (hasDelta) {
//				yearSum.setDelta(delta); TODO
			}
			// ... into json
			ArrayMap ymap = getValuesJSON2_cell(b, null, yearSum);
			String css = (String) map.get("css");
			css = (css==null?"":css)+"fontWeight:bold;";
			ymap.put("css", css);
			list.add(ymap);
		}
		return list;
	}

	ArrayMap getValuesJSON2_cell(Business b, Cell c, Numerical v) {
		// empty?
		if (v==null || v==Business.EMPTY) {
			return new ArrayMap(
					"v", 0,
					"str", ""
				);
		}
		ArrayMap map = new ArrayMap(
			"v", v.doubleValue(),
			"str", v.toString(),
			"unit", v.getUnit(),
			"comment", v.comment,			
			"css", c==null? null : b.getCSSForCell(c)
		);
		if (v.getDelta() != null) {
			map.put("delta", v.getDelta());
		}
		return map;
	}

	@Override @Deprecated
	public void addChild(ITree childNode) {
		addRule((Rule) childNode);
	}

	@Override @Deprecated
	public Object getValue() {
		return null;
	}

	@Override @Deprecated
	public void removeChild(ITree childNode) {
		throw new UnsupportedOperationException();
	}

	@Override @Deprecated
	public void setParent(ITree parent) {
		throw new UnsupportedOperationException();
	}

	@Override @Deprecated
	public void setValue(Object value) {
		throw new UnsupportedOperationException();
	}
	
}

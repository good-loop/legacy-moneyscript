package com.winterwell.moneyscript.output;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.winterwell.moneyscript.data.PlanSheet;
import com.winterwell.moneyscript.lang.AnnualRule;
import com.winterwell.moneyscript.lang.DummyRule;
import com.winterwell.moneyscript.lang.ExportCommand;
import com.winterwell.moneyscript.lang.GroupRule;
import com.winterwell.moneyscript.lang.ImportCommand;
import com.winterwell.moneyscript.lang.MetaRule;
import com.winterwell.moneyscript.lang.Rule;
import com.winterwell.moneyscript.lang.Settings;
import com.winterwell.moneyscript.lang.StyleRule;
import com.winterwell.moneyscript.lang.UncertainNumerical;
import com.winterwell.moneyscript.lang.cells.CellSet;
import com.winterwell.moneyscript.lang.cells.CurrentRow;
import com.winterwell.moneyscript.lang.cells.RowName;
import com.winterwell.moneyscript.lang.num.BasicFormula;
import com.winterwell.moneyscript.lang.num.Numerical;
import com.winterwell.moneyscript.lang.num.UnaryOp;
import com.winterwell.moneyscript.webapp.GSheetFromMS;
import com.winterwell.utils.Dep;
import com.winterwell.utils.IFilter;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.containers.ITree;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Time;

/**
 * Each line of the plan will produce a Row in the spreadsheet??
 * 
 * Lifecycle: Made late in the parse by Lang.parse()
 * 
 * @author daniel
 *
 */
public final class Row implements ITree // NB: we don't use Row ITree anywhere (yet)
{

	private static final String LOGTAG = "Row";
	String name;

	public Row getParent() {
		return parent;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Row other = (Row) obj;
		return Objects.equals(name, other.name);
	}

	public Row(String name) {
		this.name = name.trim();
		// HACK - avoid annual totals for Balance, Head Count, etc
		// ??How might we make this a setting in the script??
		// maybe "Total: annual except(Balance)"
		String cname = StrUtils.toCanonical(name).replace(" ", "");
		if (Arrays.asList("balance", "cashatbank", "cash").contains(cname) || cname.contains("count")) {
			noTotal = true;
		}
	}

	@Override
	public String toString() {
		return name;
	}

	/**
	 * rule ordering: ??
	 * @return
	 */
	public List<Rule> getRules() {
		if (parent == null) {
			return rules;
		}
		// copy this rows rules and add in the parent rules ??now done during parse
		ArrayList<Rule> rules2 = new ArrayList<Rule>(rules);
//		List<Rule> rs = parent.getRules();
//		for (Rule rule : rs) {
//			if (rule instanceof GroupRule)
//				continue;
//			rules2.add(rule);
//		}
		return rules2;	
	}

	final List<Rule> rules = new ArrayList<Rule>();
	private int index = -1;
	private List<Row> kids = new ArrayList<Row>();
	/**
	 * What about rows with 2 parents??
	 */
	private Row parent;
	private List<StyleRule> stylers = new ArrayList<StyleRule>();
	/**
	 * If true don't include a year-end total for this row e.g. Balance
	 */
	private boolean noTotal;

	/**
	 * Process the cell -- except group cells
	 * 
	 * @param col
	 * @param b
	 * @return value or null for a group cell
	 */
	public Numerical calculate(Col col, Business b) {
		if (("" + getName() + " " + this).toLowerCase().contains("gross marketing spend")) { // debug!
			System.out.println(this);
		}
		Cell cell = new Cell(this, col);
		// special case: group rows
		if (isGroup()) {
			// ??But you can also use a group for organisation, to contain a set of child
			// rows
			return getGroupValue(col, cell);
		}
		List<Rule> rs = getRules();
		if (rs.isEmpty())
			return null;
		Numerical v = null;
		// Last rule wins in a straight calculation (some rules are modifiers)
		// Minor efficiency TODO - mark which rules are modifiers, then start with the
		// last absolute rule
		for (Rule r : rs) {
			// imports win
			if (v != null && ImportCommand.isImported(v)) {
				continue;
			}
			// skip non calc rules
			if (r instanceof MetaRule)
				continue;
			if (r instanceof StyleRule)
				continue;
			if (r instanceof DummyRule)
				continue;
			if (r instanceof GroupRule) {
				continue;
			}
			assert r != null : rs;
			// NB: scenario on/off is done inside calculate
			Numerical v2 = r.calculate(cell);
			if (v2 == null) {
				continue; // e.g. rule not active yet
			}
			if (Numerical.isZero(v2)) {
				// ?? vs v
			}
			// comment = rule name
			if (v != v2) {
				// ?? How to spot when r is/isn't a modifier, and the earlier comment should be
				// kept/discarded?
				if (ImportCommand.isImported(v2)) {
					// its an import, leave it alone
				} else {
					// combine comments
					String v2c = Utils.or(v2.comment, r.src);
					v2.comment = StrUtils.space(v == null ? null : v.comment, v2c);
				}
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

	public List<Cell> getCells() {
		Business b = Business.get();
		List<Col> cols = b.getColumns();
		List<Cell> list = new ArrayList<Cell>(cols.size());
		for (Col col : cols) {
			list.add(new Cell(this, col));
		}
		return list;
	}

	public final int getIndex() {
		if (index != -1)
			return index;
		Business b = Business.get();
		index = b.getRowIndex(this);
		return index;
	}

	public boolean isOn() {
		// is there an off rule?
		Cell cell = new Cell(this, new Col(1));
		for (Rule r : rules) {
			if (r instanceof MetaRule) {
				String meta = ((MetaRule) r).meta;
				if (!"off".equals(meta))
					continue;
				boolean applies = r.getSelector().contains(cell, cell);
				if (!applies)
					continue;
				return false;
			}
		}
		return true;
	}

	/**
	 * The direct children. never null
	 */
	public final List<Row> getChildren() {
		return kids;
	}

	public void setParent(Row group) {
		if (parent == group)
			return;
		assert parent == null;
		parent = group;
		assert !group.kids.contains(this) : this;
		group.kids.add(this);
		// NB: scenario is done at the rule level (not the row level)
	}

	public final boolean isGroup() {
		return !kids.isEmpty();
	}

	/**
	 * ?? Is this 0 or 1 indexed
	 * 
	 * @return
	 */
	public double[] getValues() {
		Business b = Business.get();
		List<Cell> cells = Containers.getList(getCells());
		double[] vs = new double[cells.size()];
		for (int i = 0; i < vs.length; i++) {
			Cell c = cells.get(i);
			Numerical v = b.getCellValue(c);
			vs[i] = (v == null || v == Business.EVALUATING) ? 0 : v.doubleValue();
		}
		return vs;
	}

	public Numerical getGroupValue(Col col, Cell b) {
		assert isGroup() : this;
		// HACK na?
		List<Rule> _rules = getRules();
		for (Rule rule : _rules) {
			if (rule instanceof GroupRule && ((GroupRule) rule).isNA()) {
				Numerical n = Business.EMPTY; // HACK 0 isnt skip, but it should do for most cases
				return n;
			}
		}
		// sum over the kids
		List<Row> kids = getChildren();

		// sample from distributions? (could we preserve in some cases instead?? But
		// thats messy)
		Business biz = Business.get();
		Numerical dummy = biz.getCellValue(new Cell(kids.get(0), col));
		if (dummy instanceof UncertainNumerical) {
			return getGroupValue2_dist(col, biz);
		}

		Numerical sum = new Numerical(0);
		// add them up...
		boolean allImports = true; // stays true if every bit of the sum is an import
		int size = 0;
		for (Row kid : kids) {
			Numerical v = biz.getCellValue(new Cell(kid, col));
			assert !(v instanceof UncertainNumerical) : this; // Sampled above
			if (Numerical.isZero(v)) {
				continue;
			}
			if (!Double.isFinite(v.doubleValue())) {
				Log.w(LOGTAG, "Bad numerical " + b + " = " + v);
			}
			Numerical oldSum = sum;
			// plus
			sum = sum.plus(v);
			// ...what went into the sum?
			size += v.cnt == 0 ? 1 : v.cnt;
			String newComment = StrUtils.joinWithSkip(" + ", oldSum.comment, kid.getName() + "(" + v + ")");
			sum.comment = newComment;
			String newExcel = (oldSum.excel == null ? "" : oldSum.excel + " + ") + GSheetFromMS.cellRef(kid, col);
			sum.excel = newExcel;
			if (allImports && ! ImportCommand.isImported(v)) {
				allImports = false;
			}
		}
		// number of rows? NB not needed for a low number
		sum.cnt = size;
		if (size > 4) {
			// ?? why does this not always give the same answer?? But top level count(Foo)
			// does?? oddity seen with count(Staff) in 2022 Budget
//			UnaryOp countOp = new UnaryOp("count", new BasicFormula(new RowName(getName())));
//			Numerical cnt2 = countOp.calculate(b);
//			if (size == cnt2.intValue()) {
//				assert size != -1;
//			} else {
//				assert size != -1;
//				Numerical cnt3 = countOp.calculate(b);
//			}
			sum.comment += " (" + size + " basic rows)";
		}
		// HACK
		if (!Numerical.isZero(sum) && allImports) {
			sum.comment = "imports: " + sum.comment; // mark it for display
		}
		return sum;
	}

	private Numerical getGroupValue2_dist(Col col, Business b) {
		List<Row> kids = getChildren();
		int numParticles = 20; // a low default??
		UncertainNumerical kid0Value = (UncertainNumerical) b.getCellValue(new Cell(kids.get(0), col));
		if (kid0Value.getDist() instanceof Particles1D) {
			numParticles = ((Particles1D) kid0Value.getDist()).pts.length;
		}
		Particles1D ps = new Particles1D(numParticles);
		String unit = null;
		for (Row kid : kids) {
			UncertainNumerical v = (UncertainNumerical) b.getCellValue(new Cell(kid, col));
			if (v == null)
				continue;
			Particles1D dist = new Particles1D(numParticles, ((UncertainNumerical) v).getDist());
			assert ps.pts.length == dist.pts.length;
			for (int i = 0; i < dist.pts.length; i++) {
				ps.pts[i] += dist.pts[i];
			}
			if (unit == null)
				unit = v.getUnit();
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
	 * sort rules: Last is better.
	 * 
	 *  - TODO?? `at` rules go last
	 *  - Then stackable modifier rules like "+ 10%"
	 *  - Then rules with a filter beat those without (i.e. go later)

		This is regardless of when the rule is defined in the script.
	 * 	  
	 * 	Otherwise it's last-in wins.
	 * 
	 * @param _rules
	 */
	private void sortRules(List<? extends Rule> _rules) {
//		if (getName().contains("Gross Mark")) { // debug
//			System.out.println(_rules);
//		}
		Collections.sort(_rules, new Comparator<Rule>() {
			@Override
			public int compare(Rule a, Rule b) {
				// at rules win??
//				if (a.getSelector()) see testAtMonth()
				// modifier rules go last so they can stack
				boolean aIsMod = a.isStacked();
				boolean bIsMod = b.isStacked();
				if (aIsMod || bIsMod) {
					return Boolean.compare(aIsMod, bIsMod);
				}
				// filter rules go later so they can take over
				CellSet as = a.getSelector();
				CellSet bs = b.getSelector();
				return as.compareTo(bs);
			}
		});
//		System.out.println(_rules);
	}

	public List<MetaRule> getMetaRules() {
		return Containers.filter(rules, new IFilter() {
			public boolean accept(Object x) {
				return x instanceof MetaRule;
			}
		});
	}

	public List<Map> getValuesJSON(boolean yearTotals) {
//		if (name.contains("Grant")) {	// debug
//			System.out.println(this);
//		}
		List<Cell> cells = Containers.getList(getCells());
		Business b = Business.get();
		Settings settings = b.getSettings();
		int yearEndMonth = settings.getYearEnd();
		List<Map> list = new ArrayList<Map>();
		String unit = null;
		for (int i = 0; i < cells.size(); i++) {
			Cell c = cells.get(i);
			Numerical v = b.getCellValue(c);
			// cell json
			ArrayMap map = getValuesJSON2_cell(b, c, v);
			list.add(map);

			// year total?
			if ( ! yearTotals) continue;
			Time t = c.getColumn().getTime();
			if (t.getMonth() != yearEndMonth) {
				continue; // not time for a year end
			}
			if (v != null && v.getUnit() != null) {
				unit = v.getUnit();
			}
			Numerical yearSum = getValuesJSON_calculateYearTotal(b, c, cells, i, unit);
			if (yearSum == null) {
				list.add(new ArrayMap()); // add a blank for column balance
				continue;
			}
			// ... into json
			ArrayMap ymap = getValuesJSON2_cell(b, null, yearSum);
			String css = (String) map.get("css");
			css = (css == null ? "" : css) + "fontWeight:bold;";
			ymap.put("css", css);
			list.add(ymap);
		}
		return list;
	}

	/**
	 * 
	 * @param b
	 * @param c     The end of year cell
	 * @param cells
	 * @param i     c = cells[i]
	 * @param unit
	 * @return
	 */
	private Numerical getValuesJSON_calculateYearTotal(Business b, Cell c, List<Cell> cells, int i, String unit) {
		Time t = c.getColumn().getTime();
		assert t.getMonth() == b.getSettings().getYearEnd() : t;
		// Is there an annual rule?
		AnnualRule ar = Containers.firstClass(getRules(), AnnualRule.class);
		if (ar == null) {
			// avoid by default for some rows e.g. balance
			if (noTotal) {
				return null;
			}
			// HACK: also treat %s as noTotal
			if ("%".equals(unit)) {
				// NB: We can't average as it's not clear if/how to weight the entries
				// TODO Can we port the formula from per-month to per-year??
				// No - formulas work on Cells and BusinessState
				// So we want to include year-total columns in the BusinessState (bleurgh)
				noTotal = true;
				return null;
			}
			// default to sum
			ar = new AnnualRule(new RowName(getName()), "sum", "");
		}

		// Apply!
		Numerical yearSum = ar.calculateAnnual(b, cells, i);
		return yearSum;
	}

	ArrayMap getValuesJSON2_cell(Business b, Cell c, Numerical v) {
		// empty?
		if (v == null || v == Business.EMPTY) {
			return new ArrayMap("v", 0, "str", "");
		}
		double dv = v.doubleValue();
//		GSheetFromMS gs = c==null? null : ExportCommand.getGSheetFromMSForRow(c.row);
//		String exref = c==null || gs==null? null : gs.exportCellRefs(gs.cellRef(c.row, c.col), null);
		ArrayMap map = new ArrayMap(
				"v", Double.isFinite(dv) ? dv : null, 
				"str", v.toString(), 
				"unit", v.getUnit(),
				"comment", StrUtils.joinWithSkip("; ", v.comment, dv, v.excel), // exref,  
				"css", c == null ? null : b.getCSSForCell(c));
		if (v.getDelta() != null) {
			map.put("delta", v.getDelta());
		}
		if (v.excel != null) {
//			PlanSheet contextPlanSheet = b.getPlanSheetForRow(c.row);
			map.put(ExportCommand.EXCEL_WITH_MS_CELL_REFS, v.excel); // not exported yet!
//					gs==null? v.excel : gs.exportCellRefs(v.excel, contextPlanSheet));
		}
		return map;
	}

	@Override
	@Deprecated
	public void addChild(ITree childNode) {
		addRule((Rule) childNode);
	}

	@Override
	@Deprecated
	public Object getValue() {
		return null;
	}

	@Override
	@Deprecated
	public void removeChild(ITree childNode) {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public void setParent(ITree parent) {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public void setValue(Object value) {
		throw new UnsupportedOperationException();
	}

}

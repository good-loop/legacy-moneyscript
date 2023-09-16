package com.winterwell.moneyscript.lang.num;

import java.util.Set;

import com.winterwell.maths.stats.distributions.d1.UniformDistribution1D;
import com.winterwell.moneyscript.lang.UncertainNumerical;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.webapp.GSheetFromMS;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.containers.ArraySet;
import com.winterwell.utils.containers.Range;
import com.winterwell.utils.containers.Tree;
import com.winterwell.utils.log.Log;

class BinaryOp extends Formula {

	private Formula left;
	private Formula right;
	
	@Override
	public Tree<Formula> asTree() {
		Tree t = new Tree(this);
		Tree tl = left.asTree();
		Tree tr = right.asTree();
		tl.setParent(t);
		tr.setParent(t);
		return t;
	}
	
	@Override
	public boolean isStacked() {
		return left.isStacked() || right.isStacked();
	}
	
	public BinaryOp(String op, Formula left, Formula right) {
		super(op);
//		assert ! (left instanceof SetValueFormula);
//		assert ! (right instanceof SetValueFormula);
		this.left = left;
		this.right = right;
		assert op != null;
		assert left != null && right != null;
	}
	
	@Override
	public Set<String> getRowNames(Cell focus) {
		ArraySet<String> set = new ArraySet<String>(left.getRowNames(focus));
		set.addAll(right.getRowNames(focus));
		return set;
	}

	// TODO preserve hashtag breakdown (which does already happen for some formulae below)
	@Override
	public Numerical calculate(Cell b) {
		Numerical x = left.calculate(b);
		if (x==null) x = Numerical.NULL;
		Numerical y = right.calculate(b);
		if (y==null) {
			if (x==Numerical.NULL) return null;
			y = Numerical.NULL;
		}
		
		// evaluating = 0? Sometimes. e.g. a payrise rule like `Staff: * 110%` vs a staff member starting later will
		// trigger an issue when applied to `Alice from month 3: £20k per year` -- as month 1 is 110% of evaluating.
		// This case is handled by treating as 0 (and not an error)
		if (x==Business.EVALUATING && left instanceof BasicFormula && ((BasicFormula) left).isCurrentRow()) {
			x = new Numerical(0);
		}
		if (y==Business.EVALUATING && right instanceof BasicFormula && ((BasicFormula) right).isCurrentRow()) {
			y = new Numerical(0);
		}
		
		if (x instanceof UncertainNumerical) Log.report("unexpected Uncertain: "+x+" from "+left);
		if (y instanceof UncertainNumerical) Log.report("unexpected Uncertain: "+y+" from "+right);

		switch(op) {
		case "-":
			// copy-pasta of "+"
			// special case: e.g. £10 - 20% = £8
			if ("%".equals(y.getUnit()) && ! "%".equals(x.getUnit())) {
				Numerical one_y = new Numerical(1).minus(y);
				Numerical xty = x.times(one_y);
				xty.excel = GSheetFromMS.excelbon(x)+" * "+GSheetFromMS.excelbon(one_y); // TODO pass on y's excel
				return xty;
			}
			Numerical xmy = x.minus(y);			
			xmy.excel = GSheetFromMS.excel(x)+" - "+GSheetFromMS.excelb(y);

//			// handle minus as + -1*y -- so that we get the same % handling behaviour
//			Numerical oldy = y;
//			y = oldy.times(-1);
//			if (oldy.excel!=null) {
//				y.excel = "-1*"+GSheetFromMS.excelb(oldy);
//			}
			return xmy;
		case "+":
			// special case: e.g. £10 + 20% = £12
			if ("%".equals(y.getUnit()) && ! "%".equals(x.getUnit())) {
				Numerical yplus1 = y.plus(1);
				Numerical xty = x.times(yplus1);
				xty.excel = GSheetFromMS.excelbon(x)+" * "+GSheetFromMS.excelbon(yplus1); // TODO pass on y's excel
				return xty;
			}
			Numerical xny = x.plus(y);			
			xny.excel = GSheetFromMS.excel(x)+" + "+GSheetFromMS.excel(y);
			return xny;
		case "*":
			if (x==Numerical.NULL || y==Numerical.NULL) return null;
			Numerical xy = x.times(y);
			xy.excel = GSheetFromMS.excelbon(x)+" * "+GSheetFromMS.excelbon(y);
			return xy;
		case "@":	// like *, but preserves the LHS value for access
			if (x==Numerical.NULL || y==Numerical.NULL) return null;
			Numerical n = x.times(y);
			return new Numerical2(n, x);
		case "/":
			if (x==Numerical.NULL) return null;
			// what to do with divide by zero?
			Numerical xdy = x.divide(y);
			xdy.excel = GSheetFromMS.excelbon(x)+"/"+GSheetFromMS.excelbon(y);
			return xdy;
		case "+-": case "±":
			Range range = new Range(x.doubleValue()-y.doubleValue(), x.doubleValue()+y.doubleValue());
			UniformDistribution1D dist = new UniformDistribution1D(range);
			return sample(new UncertainNumerical(dist, DefaultCalculator.unit(x, y)));		
		case"min":
			double mxy = Math.min(x.doubleValue(), y.doubleValue());
			Numerical winner = mxy==x.doubleValue()? x : y;
			return winner; //new Numerical(mxy, DefaultCalculator.unit(x, y));
		case "max":
			mxy = Math.max(x.doubleValue(), y.doubleValue());
			winner = mxy==x.doubleValue()? x : y;
			return winner;
		case "power": case "^":
			if (x==Numerical.NULL) return null;
			double xtoy = Math.pow(x.doubleValue(), y.doubleValue());
			return new Numerical(xtoy, x.getUnit()); // Keep the x unit
		}
		throw new TodoException(toString());
	}
	
	@Override
	public String toString() {
		return "("+left+op+right+")";
	}
}
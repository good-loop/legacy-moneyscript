package com.winterwell.moneyscript.lang.num;

import java.util.Set;

import com.winterwell.moneyscript.lang.bool.Condition;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.containers.ArraySet;
import com.winterwell.utils.containers.Tree;

public class ConditionalFormula extends Formula {

	private Formula then;
	private Formula other;

	@Override
	public Tree<Formula> asTree() {
		Tree t = new Tree(this);
		Tree tl = then.asTree();
		Tree tr = other.asTree();
		tl.setParent(t);
		tr.setParent(t);
		return t;
	}
	
	@Override
	public Set<String> getRowNames(Cell focus) {
		ArraySet<String> set = new ArraySet<String>(then.getRowNames(focus));
		set.addAll(other.getRowNames(focus));
		return set;
	}
	
	public ConditionalFormula(Condition tst, Formula then, Formula other) {
		super("if");
		this.then = then;
		this.other = other;
	}

	@Override
	public Numerical calculate(Cell b) {
		// TODO Auto-generated method stub
		throw new TodoException(this);
	}

	@Override
	public boolean isStacked() {		
		return then.isStacked() || other.isStacked();
	}

}

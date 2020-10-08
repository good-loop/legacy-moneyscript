package com.winterwell.moneyscript.lang.num;

import java.util.Set;

import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Col;
import com.winterwell.moneyscript.output.Row;
import com.winterwell.moneyscript.lang.bool.Condition;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.containers.ArraySet;

public class ConditionalFormula extends Formula {

	private Formula then;
	private Formula other;

	@Override
	public Set<String> getRowNames() {
		ArraySet<String> set = new ArraySet<String>(then.getRowNames());
		set.addAll(other.getRowNames());
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

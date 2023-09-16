package com.winterwell.moneyscript.lang.num;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.winterwell.moneyscript.lang.time.DtDesc;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Tree;
import com.winterwell.utils.time.Dt;

public class PerFormula extends Formula {

	DtDesc dt;
	String op;
	private Formula n;
	
	@Override
	public Tree<Formula> asTree() {
		Tree t = new Tree(this);
		Tree tl = dt.f.asTree();
		Tree tr = n.asTree();
		tl.setParent(t);
		tr.setParent(t);
		return t;
	}
	

	public PerFormula(Formula n, String op, DtDesc dt) {
		super(op+"-"+dt);
		Utils.check4null(n, op, dt);
		this.op = op;
		this.n = n;
		this.dt = dt;
	}

	@Override
	public String toString() {	
		return n+" per "+dt;
	}
	
	@Override
	public Numerical calculate(Cell b) 
	{
		// are we in the period? TODO over
		if (op.equals("over")) throw new TodoException(""+this);
		// sample n
		Numerical val = n.calculate(b);
		// get period
//		Dt dtVal = dt.calculate(row, col, b);
		// Convert n into the right timestep
		Dt step = b.getBusiness().getTimeStep();
		double m = dt.calculate(b).divide(step);		
		Numerical bit = val.times(1/m);
		return sample(bit);
	}

}

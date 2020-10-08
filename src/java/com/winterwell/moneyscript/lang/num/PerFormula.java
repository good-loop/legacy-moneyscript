package com.winterwell.moneyscript.lang.num;

import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Col;
import com.winterwell.moneyscript.output.Row;
import com.winterwell.moneyscript.lang.time.DtDesc;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;

public class PerFormula extends Formula {

	DtDesc dt;
	String op;
	private Formula n;

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

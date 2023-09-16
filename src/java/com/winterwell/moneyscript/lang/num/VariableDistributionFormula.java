package com.winterwell.moneyscript.lang.num;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.BusinessContext;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Row;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.containers.Tree;

public class VariableDistributionFormula extends Formula {
	
	String var;
	private String group;
	private Formula right;

	public String getVar() {
		return var;
	}
	
	public VariableDistributionFormula(String var, String group, Formula formula) {
		super("in");
		this.var = var;
		this.group = group;
		this.right = formula;
	}

	@Override
	public Tree<Formula> asTree() {
		Tree t = new Tree(this);
		Tree<Formula> tr = right.asTree();
		tr.setParent(t);
		return t;
	}
	
	/**
	 * Perform a sum over the group values
	 */
	@Override
	public Numerical calculate(Cell b) {
		Business biz = b.getBusiness();
		Row groupRow = biz.getRow(group);
		if (groupRow==null) throw new IllegalArgumentException("No such group: "+group);
		List<Row> kids = groupRow.getChildren();
		Numerical sum = new Numerical(0);
		for(Row row : kids) {
			Cell rc = new Cell(row, b.col);
			Row prev = biz.putRow4Name(var, row); // set
			Numerical weight = biz.getCellValue(rc);
			if (Numerical.isZero(weight)) continue;
			BinaryOp bop = new BinaryOp("*", new BasicFormula(weight), right);
			Numerical rowVal = bop.calculate(rc);
			sum = sum.plus(rowVal);
			biz.putRow4Name(var, prev); // reset
		}
		return sum;
	}

}

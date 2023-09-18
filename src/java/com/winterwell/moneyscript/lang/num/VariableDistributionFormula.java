package com.winterwell.moneyscript.lang.num;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.BusinessContext;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Row;
import com.winterwell.moneyscript.output.VarSystem;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.containers.Tree;

/**
 * Loop over options in a group
 * e.g. [Region in Region Mix: Region.Price]
 * @author daniel
 *
 */
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
		VarSystem vars = biz.getVars();
		Row groupRow = biz.getRow(group);
		if (groupRow==null) throw new IllegalArgumentException("No such group: "+group);
		List<Row> kids = groupRow.getChildren();		
		Numerical sum = new Numerical(0);
		for(Row row : kids) {
			Cell rc = new Cell(row, b.col);
//			try (reset) TODO refactor for consistency
			String prev = vars.setRow4Name(var, row.getName());
			Numerical weight = biz.getCellValue(rc);
			if ( ! Numerical.isZero(weight)) {
				BinaryOp bop = new BinaryOp("*", new BasicFormula(weight), right);
				Numerical rowVal = bop.calculate(b);
				sum = sum.plus(rowVal);
			}
			vars.setRow4Name(var, prev); // reset
		}
		return sum;
	}

	@Override
	public String toString() {
		return "VariableDistributionFormula [var=" + var + ", group=" + group + ", right=" + right + "]";
	}

}

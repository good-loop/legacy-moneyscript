package com.winterwell.moneyscript.lang.num;
//package winterwell.moneyscript.lang;
//
///**
// * set a value once and once only
// * @author daniel
// *
// */
//public class SetValueFormula extends Formula {
//
//	private Numerical v;
//
//	public SetValueFormula(Numerical v) {
//		super(":=");
//		this.v = v;
//	}
//
//	@Override
//	public Numerical calculate(Cell cell, Cell b) {
//		Rule rule = Context.getActiveRule();
//		assert rule != null;
//		Col start = rule.selector.getStartColumn(row);
//		if (start==null) {
//			return col.index==1? sample(v) : null;
//		}
//		if (col.equals(start)) return sample(v);
//		return null;
//	}
//
//
//}

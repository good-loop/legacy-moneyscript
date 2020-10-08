package com.winterwell.moneyscript.lang.bool;

import static com.winterwell.nlp.simpleparser.Parsers.chain;
import static com.winterwell.nlp.simpleparser.Parsers.lit;
import static com.winterwell.nlp.simpleparser.Parsers.opt;
import static com.winterwell.nlp.simpleparser.Parsers.ref;
import static com.winterwell.nlp.simpleparser.Parsers.seq;
import static com.winterwell.nlp.simpleparser.Parsers.space;

import java.util.List;

import com.winterwell.moneyscript.lang.num.Formula;
import com.winterwell.moneyscript.lang.num.LangNum;
import com.winterwell.nlp.simpleparser.AST;
import com.winterwell.nlp.simpleparser.PP;
import com.winterwell.nlp.simpleparser.ParseResult;
import com.winterwell.nlp.simpleparser.Parser;

public class LangBool {
	
	public static final Parser<Condition> bool = ref("bool");
	
	private PP<String> comparison = new PP<String>(seq(opt(space), lit(">","<","==","<=",">="), opt(space))) {
		protected String process(ParseResult r) {
			AST<?> ast = (AST<?>) r.ast.getLeaves().get(0);
			return ast.parsed();
		};
	};

	private PP<Condition> test0 = new PP<Condition>(seq(LangNum.num, comparison, LangNum.num)) {
		@Override
		protected Condition process(ParseResult r) {
			List<AST> leaves = r.getLeaves();
			assert leaves.size() == 3 : leaves;
			Formula lhs = (Formula) leaves.get(0).getX();
			String cmp = (String) leaves.get(1).getX();
			Formula rhs = (Formula) leaves.get(2).getX();
			return new Comparison(lhs, cmp.trim(), rhs);
		}			
	};
	
	private PP<Condition> test = new PP<Condition>(
			chain(test0, seq(space, lit("and","or"), space))
	) {
		protected Condition process(ParseResult r) {
			List<AST> leaves = r.getLeaves();
			if (leaves.size() == 1) {
				return (Condition) leaves.get(0).getX();
			}
			assert leaves.size() == 3 : leaves;
			String o = leaves.get(1).parsed();
			Condition a = (Condition) leaves.get(0).getX();
			Condition b = (Condition) leaves.get(2).getX();
			return new Combi(a, o, b);
		}
	}.label(bool.getName());


	public LangBool() {	
	}
}

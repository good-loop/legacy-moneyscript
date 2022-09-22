package com.winterwell.moneyscript.lang.num;

import static com.winterwell.nlp.simpleparser.Parsers.bracketed;
import static com.winterwell.nlp.simpleparser.Parsers.first;
import static com.winterwell.nlp.simpleparser.Parsers.ignore;
import static com.winterwell.nlp.simpleparser.Parsers.lit;
import static com.winterwell.nlp.simpleparser.Parsers.opt;
import static com.winterwell.nlp.simpleparser.Parsers.optSpace;
import static com.winterwell.nlp.simpleparser.Parsers.ref;
import static com.winterwell.nlp.simpleparser.Parsers.regex;
import static com.winterwell.nlp.simpleparser.Parsers.seq;
import static com.winterwell.nlp.simpleparser.Parsers.space;
import static com.winterwell.nlp.simpleparser.Parsers.word;
import com.winterwell.datalog.server.CurrencyConvertor;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;

import com.winterwell.maths.stats.distributions.d1.Gaussian1D;
import com.winterwell.moneyscript.lang.UncertainNumerical;
import com.winterwell.moneyscript.lang.bool.Condition;
import com.winterwell.moneyscript.lang.bool.LangBool;
import com.winterwell.moneyscript.lang.cells.CellSet;
import com.winterwell.moneyscript.lang.cells.CurrentRow;
import com.winterwell.moneyscript.lang.cells.LangCellSet;
import com.winterwell.moneyscript.lang.time.DtDesc;
import com.winterwell.moneyscript.lang.time.LangTime;
import com.winterwell.nlp.simpleparser.AST;
import com.winterwell.nlp.simpleparser.ChainParser;
import com.winterwell.nlp.simpleparser.PP;
import com.winterwell.nlp.simpleparser.ParseFail;
import com.winterwell.nlp.simpleparser.ParseResult;
import com.winterwell.nlp.simpleparser.Parser;
import com.winterwell.utils.Dep;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Slice;
import com.winterwell.utils.containers.Tree;

/**
 * numbers and formulae
 * @author daniel
 *
 */
public class LangNum {

	PP<Formula> globalVars = new PP<Formula>(
			first(word("row"),word("column"),word("month"),word("previous"),word("year"))) 
	{
		@Override
		protected Formula process(ParseResult<?> r) {			
			return new Var(r.parsed());
		}		
	};
	
	private static final String NUMBER = "num";
	
	public static final Parser<Formula> num = ref(NUMBER);

	Parser<String> opTightBind = seq(optSpace,lit("+-", "±", "^"), optSpace);
	Parser<String> opMediumBind = seq(optSpace,lit("*", "/", "@"), optSpace);
	Parser<String> opPlusBind = seq(optSpace,lit("+", "-"), optSpace);	
	/**
	 * e.g. +
	 */
	Parser<String> opAny = first(opTightBind, opMediumBind, opPlusBind).label("opAny");

	
	PP<Numerical> plainNumber = new PPPlainNumber().label("123");
	
	static class PPPlainNumber extends PP<Numerical> {
		
		public PPPlainNumber() {
			super(regex(Numerical.number.pattern()));
		}

		protected Numerical process(ParseResult<?> r) {
			Numerical n = new Numerical(r.parsed());
			// HACK: handle dollars if a convertor was set for that
			if ("$".equals(n.getUnit())) {
				CurrencyConvertor_USD2GBP cc = Dep.getWithDefault(CurrencyConvertor_USD2GBP.class, null);				
				if (cc !=null) {
					double v = n.doubleValue();
					double v2 = cc.convertES(v);
					Numerical n2 = new Numerical(v2);
					n2.setUnit("£");
					return n2;
				}
			}
			return n;
		}
	};
	
	/** no comma! plainNumber's regex grabs the , which breaks the parse for gaussian */
	PP<Numerical> plainNumber2 = new PP<Numerical>(regex("-?(£|$)?([0-9]+\\.?\\d*)(k|m|bn)?%?")) {
		protected Numerical process(ParseResult<?> r) {
			return new Numerical(r.parsed());
		}
	}.label("123v2");
			
	/**
	 * N(mean,variance)
	 */
	Parser<Numerical> gaussian = new PP<Numerical>(seq(lit("N(").label(null), plainNumber2, regex("\\s*,\\s*").label(null), plainNumber2, lit(")").label(null))) {
		protected Numerical process(ParseResult<?> r) {
//			AST gn = r.getNode(gaussian);
			List<AST> ls = r.getLeaves();
			Numerical a = (Numerical) ls.get(0).getX();
			Numerical b = (Numerical) ls.get(1).getX();
			assert a.getClass() == Numerical.class;
			assert b.getClass() == Numerical.class;
			Gaussian1D dist = new Gaussian1D(a.doubleValue(), b.doubleValue());
			return new UncertainNumerical(dist, a.getUnit());			
		}
	}.eg("N(£10, 1)");


	/**
	 * lowercase and including the hash e.g. "#uk"
	 */
	public static Parser<String> hashTag = new PP<String>(regex("#[a-zA-Z0-9]+")) {
		@Override
		protected String process(ParseResult<?> r) throws ParseFail {
			MatchResult htag = (MatchResult) r.getX();
			return htag.group().toLowerCase();
		}		
	}.eg("#green").label("hashtag");
	
	/**
	 * A cell set gets evaluated to a number in context.
	 * We only allow single-row cell sets -- multiple row sets would not
	 * evaluate nicely. 
	 */
	PP<Formula> cellSetAsFormula = new PP<Formula>(
			seq(
					first(LangCellSet.cellSet1Row, LangCellSet.cellSetFilter)
					, opt(hashTag))
			) {
		@Override
		protected Formula process(ParseResult<?> r) {
//			Utils.breakpoint();
			List ls = r.getLeafValues();
			CellSet sel = (CellSet) ls.get(0); 			
			BasicFormula bf = new BasicFormula(sel);
			if (ls.size()==2) {
				String htag = (String) ls.get(1);
				bf.setTag(htag);
			}
			return bf;
		}
	}.label("cellSetAsFormula");

	
	Parser<Numerical> _number = first(gaussian, plainNumber)
									.label(NUMBER); // temporary assignment for simple egs in this class


	final Parser<String> mathFnNameUnary = word(
			"count row", "count",
			"sum row", "sum", "mean", "log", 
			"round down", "round up", "round", 
			"sqrt", "abs", 
			"previous", "p",
			"average"
			).label("mathFnNameUnary");


	/** TODO if then else formulae */		
	Parser<Formula> conditionalFormula = new PP<Formula>(
			seq(lit("if").label(null), space, LangBool.bool, space, 
				lit("then").label(null), space, num,
				opt(seq(space, lit("else").label(null), space, num)))) 
	{
		protected Formula process(ParseResult<?> r) {
			Condition tst = r.getNode(LangBool.bool).getX();
			List<AST> leaves = r.getLeaves();
			assert leaves.size() == 2 || leaves.size() == 3 : leaves;
			Formula then = (Formula) leaves.get(1).getX();
			Formula other = leaves.size() == 2? null : (Formula) leaves.get(2).getX();
			return new ConditionalFormula(tst, then, other);
		}
	};

	Parser<Formula> mathFnUnaryNormal = seq(mathFnNameUnary, ignore("("), optSpace, num, optSpace, ignore(")"))
										.label("mathFnUnaryNormal");
	
	Parser<Formula> mathFnUnary = new PP<Formula>(first(
			mathFnUnaryNormal,
			// because binding precedence gets nasty, only allow this for the 
			// simple "sum Sales" or "count Staff" case
			seq(mathFnNameUnary, space, cellSetAsFormula)
			// ?? special case hack for #Row?? (no space)
			)) 
	{
		@Override
		protected Formula process(ParseResult<?> r) {
			AST<Formula> fn = r.getNode(mathFnUnaryNormal);
			if (fn!=null) {
				List<Tree<Slice>> ls = fn.getLeaves();
				AST<String> op = fn.getNode(mathFnNameUnary);
				AST<Formula> args = (AST<Formula>) fn.getChildren().get(1);
				return new UnaryOp(op.parsed(), args.getX());
			} else {
				AST<String> op = r.getNode(mathFnNameUnary);
				AST<Formula> args = (AST<Formula>) r.getNode(cellSetAsFormula);
				return new UnaryOp(op.parsed(), args.getX());
			}			
		}
	};
	Parser<String> mathFnNameBinary = lit("max","min"); // should these be unary as well??
	Parser<Formula> mathFnBinary = new PP<Formula>(			
			seq(mathFnNameBinary, lit("(").label(null),optSpace,
			num,optSpace,lit(",").label(null),optSpace,num,
			optSpace,lit(")").label(null))
			) {
		@Override
		protected Formula process(ParseResult<?> r) {
			List<AST> ls = r.getLeaves();
			String o = ls.get(0).parsed();
			Object left = ls.get(1).getX();
			Object right = ls.get(2).getX();
			return new BinaryOp(o, (Formula) left, (Formula) right);
		}
	};
	
	/**
	 * TODO allow javascript functions to be defined in-script
	 * to extend.
	 */
	Parser<Formula> mathFn = new PP<Formula>(seq(word("function"))) {
		@Override
		protected Formula process(ParseResult<?> r) throws ParseFail {
			// TODO Auto-generated method stub
			return null;
		}
	};
			
	/**
	 * HACK: the mortgage formula
	 */
	Parser<Formula> mathFnSpecial = new PP<Formula>(			
			seq(ignore("repay("), optSpace,
					num, optSpace, 
					ignore(","), optSpace,
					num, optSpace, 
					ignore(","), optSpace,
					LangTime.dt, optSpace, 
					ignore(")"))
			) {
		@Override
		protected Formula process(ParseResult<?> r) {
			List<AST> ls = r.getLeaves();
			Object capital = ls.get(0).getX();
			Object interest = ls.get(1).getX();
			Object period = ls.get(2).getX();
			return new MortgageFormula((Formula) capital, (Formula) interest, (DtDesc) period);
		}
	};

	
	private Parser<Formula> binaryOp(Parser<?> lhs, Parser<String> op, Parser<?> rhs) {
		assert lhs != null && op != null && rhs !=null;
		return new PP<Formula>(seq(lhs,op,rhs)) {
			@Override
			protected Formula process(ParseResult<?> r) {
				List<AST> ls = r.getLeaves();
				assert ls.size() == 3 : ls;
				Formula left = (Formula) ls.get(0).getX();
				String op = (String) ls.get(1).getX();
				Formula right = (Formula) ls.get(2).getX();
				// FIXME I'm sure this can be broken -- and also done better
				// special case hack!
				// (1 - 2 - 3) parses as 1 - (2 - 3), which would give the wrong answer
				if (op.equals("-") && right instanceof BinaryOp &&
						(right.op.equals("+") || right.op.equals("-"))) 
				{
					// swap the sign to get (1 - 2 - 3) = 1 - (2 + 3)
					right.op = right.op.equals("+")? "-" : "+";
				}
				BinaryOp bop = new BinaryOp(op, left, right);
				return bop;
			}
		};
	}
		
	PP<Formula> numAsFormula = new PP<Formula>(_number) {
		@Override
		protected Formula process(ParseResult<?> r) {
			Numerical sel = (Numerical) r.ast.getX();
			return new BasicFormula(sel);
		}
	};		

	Parser<Formula> formulaValue = first( 
			numAsFormula,							
			cellSetAsFormula,
			// enforced brackets
			seq(lit("(").label(null), num, lit(")").label(null)),
			mathFnUnary, mathFnBinary, mathFnSpecial,
			// after mathFnUnary 'cos of the overlap
			globalVars
			);		
	
	Parser<Formula> formulaTight = first(
			binaryOp(formulaValue, opTightBind, ref("fTight")),
			formulaValue).label("fTight");
	Parser<Formula> formulaTimes = first(
			binaryOp(formulaTight, opMediumBind, ref("fTimes")), 
			formulaTight).label("fTimes");
	Parser<Formula> formulaPlus = first(
			binaryOp(formulaTimes, opPlusBind, ref("fPlus")), 
			formulaTimes).label("fPlus");
	// £10k per month TODO formulas for time
	Parser<Formula> formulaPerMonth = new PP<Formula>(
			seq(formulaPlus, opt(seq(space, lit("per").label(null), space, LangTime.dt)))
	) {
		@Override
		protected Formula process(ParseResult<?> r)
				throws ParseFail {
			List<AST> ls = r.getLeaves();
			if (ls.size()==1) return (Formula) ls.get(0).getX();
			// 2 per month
			Formula f = (Formula) ls.get(0).getX();
			DtDesc unit = (DtDesc) ls.get(1).getX();
			return new PerFormula(f, "per", unit);
		}			
	}.label("fPer"); //.eg("10 per month"); breaks if time isnt loaded yet
	
	Parser<Formula> formula2 = bracketed("(", formulaPerMonth , ")");
	// recursion setup to handle (1+1)*2, where formula2 will parse (1+1) and stop
	
	public Parser<Formula> formula = new ChainParser(formula2, opAny) {
		protected Formula process(ParseResult parsed) {			
			List<AST> ls = parsed.getLeaves();
			if (ls.size() == 1) {
				return (Formula) ls.get(0).getX();
			}
			// FIXME This only works if its all + or all *!
			// TODO scan for close bound, merge
			Formula fn = (Formula) ls.get(0).getX();
			for (int i=1; i<ls.size(); i += 2) {				
				String op = ls.get(i).parsed();				
				Formula right = (Formula) ls.get(i+1).getX();
				fn = new BinaryOp(op, fn, right);
			}
			return (Formula) fn;
		}
	}.label(NUMBER); 
	

	/**
	 * e.g. Staff from year 2: + 20%
	 */
	public Parser<Formula> compoundingFormula = new PP<Formula>(seq(opAny, formula)) {
		protected Formula process(ParseResult<?> r) {
			AST<?> o = (AST) r.ast.getLeaves().get(0);
			Formula f = (Formula) r.getNode(formula).getX();
			Formula left = new BasicFormula(new CurrentRow(null));
			return new BinaryOp(o.parsed(), left, f);
		}			
	}.eg("+ 20%");

	/**
	 * For inputing a list of values, 1 per column.
	 * A useful, if not entirely elegant, bridge to a more spreadsheet style of things.
	 * Capped at 1,000 columns.
	 * This is not part of num - it is invoked specially
	 */
	public Parser<List<Formula>> numList = new PP<List<Formula>>(
			new ChainParser(num, regex(",\\s*").label(null), 2, 1000)) {
		@Override
		protected List<Formula> process(ParseResult<?> r) throws ParseFail {
			List<AST> ls = r.getLeaves();			
			List<Formula> fs = new ArrayList<Formula>(ls.size());
			for (AST ast : ls) {
				Formula f = (Formula) ast.getX();
				fs.add(f);
			}
			return fs;
		}		
	}.label("numList");
}

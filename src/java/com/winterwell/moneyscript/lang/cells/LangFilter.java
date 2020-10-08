package com.winterwell.moneyscript.lang.cells;

import static com.winterwell.nlp.simpleparser.Parsers.bracketed;
import static com.winterwell.nlp.simpleparser.Parsers.chain;
import static com.winterwell.nlp.simpleparser.Parsers.first;
import static com.winterwell.nlp.simpleparser.Parsers.lit;
import static com.winterwell.nlp.simpleparser.Parsers.opt;
import static com.winterwell.nlp.simpleparser.Parsers.ref;
import static com.winterwell.nlp.simpleparser.Parsers.regex;
import static com.winterwell.nlp.simpleparser.Parsers.seq;
import static com.winterwell.nlp.simpleparser.Parsers.space;
import static com.winterwell.nlp.simpleparser.Parsers.word;

import java.util.ArrayList;
import java.util.List;

import com.winterwell.moneyscript.lang.bool.Condition;
import com.winterwell.moneyscript.lang.bool.LangBool;
import com.winterwell.moneyscript.lang.time.DtDesc;
import com.winterwell.moneyscript.lang.time.LangTime;
import com.winterwell.moneyscript.lang.time.TimeDesc;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.nlp.simpleparser.AST;
import com.winterwell.nlp.simpleparser.PP;
import com.winterwell.nlp.simpleparser.ParseResult;
import com.winterwell.nlp.simpleparser.Parser;
import com.winterwell.utils.TodoException;

/**
 * @testedby {@link LangFilterTest}
 * @author daniel
 *
 */
public class LangFilter {

	private static final String FILTER = "filter";
	
	public static final Parser<Filter> filter = ref(FILTER);

	


	Parser<Filter> conditionalFilter = new PP<Filter>(
			
			seq(lit(LangTime.from, "to", "if", "at").label("op"), space, 
					first(LangBool.bool, LangTime.time))
	) {
		protected Filter process(ParseResult<?> r) {
			String op = r.getNode("op").parsed();
			AST guts = r.getLeaves().get(1);
			Object tst = guts.getX();
			if (tst instanceof Condition) {
				return new ConditionalFilter(op, (Condition) tst);
			}			
			return new TimeFilter(op, (TimeDesc) tst);
		};
	}.label("conditionalFilter").eg("from month 3");

	/**
	 * beware of danger vs comment //
	 */
	Parser createRegex = regex("\\/[^//]+\\/");
	
	/**
	 * use with a group e.g. Staff matching /UK/
	 */
	Parser<Filter> textMatchFilter = new PP<Filter>(
			seq(lit("matching"), space, createRegex)
	) {
		protected Filter process(ParseResult<?> r) {
			AST cr = r.getNode(createRegex);
			Condition mcond = new Condition() {
				@Override
				public boolean contains(Cell cell, Cell b) {
					throw new TodoException(cr+" "+cell+" "+b);
				}
			};
			return new ConditionalFilter("matching", mcond);			
		};
	}.label("textMatchFilter");

	
	/**
	 * E.g. "for 2 months"
	 */
	private Parser<Filter> periodFilter = new PP<Filter>(
			seq(word("for"), space, first(LangTime.dt))
	) {
		protected Filter process(ParseResult<?> r) {
			AST guts = r.getLeaves().get(1);
			DtDesc dt = r.getX(LangTime.dt);
			return new PeriodFilter(dt);
		};
	}.label("periodFilter");
	
	/**
	 * E.g. "each year"
	 */
	Parser<Filter> periodicFilter = new PP<Filter>(
			seq(
					word("each").label(null), space, first(LangTime.dt),
					// e.g. "from month 3" -- this allows for managing e.g. when the year starts
					opt(seq(space, word("from").label(null), space, LangTime.time))
				)
	) {
		protected Filter process(ParseResult<?> r) {
			List guts = r.getLeafValues();
			DtDesc dt = r.getX(LangTime.dt);
			if (guts.size()==1) {
				return new EachFilter(dt, null);
			}
			TimeDesc from = r.getX(LangTime.time);
			return new EachFilter(dt, from);
		};
	};
	
	/**
	 * E.g. "above"
	 */
	Parser<Filter> dirnFilter = new PP<Filter>(word("above")) {
		protected Filter process(ParseResult<?> r) {
			String w = r.parsed();
			if (w.equals("above")) {
				return new Above();
			}
			throw new TodoException(w);
		}
	}.label("dirnFilter").eg("above");
	
	
	Parser<Filter> filter0 = bracketed("(",
			first(conditionalFilter, dirnFilter, periodFilter, periodicFilter, textMatchFilter).label("cond/dirn/period/periodic/text"),
			")")
			.label("filter0");//.setDesc("(?conditionalFilter/dirnFilter/periodFilter)?");

	/**
	 * Set "chain of filters" as the recogniser for {@link LangFilter#filter}
	 */
	Parser<Filter> filters = new PP<Filter>(chain(filter0, space)) 
	{
		@Override
		protected Filter process(ParseResult pr) {
			List<AST<Filter>> leaves = (List) pr.ast.getLeaves();
			if (leaves.size() == 1) {
				// a single filter (the normal case)
				return leaves.get(0).getX();
			}
			// a chain of filters
			ArrayList<Filter> filters = new ArrayList<Filter>(leaves.size());
			for (AST<Filter> ast : leaves) {
				Filter f = ast.getX();
				filters.add(f); 
			}			
			return new Chain(filters);
		}		
	}.label(FILTER).setDesc("chain of filter0");
	

}

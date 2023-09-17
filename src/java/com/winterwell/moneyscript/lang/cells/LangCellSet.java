package com.winterwell.moneyscript.lang.cells;


import static com.winterwell.nlp.simpleparser.Parsers.bracketed;
import static com.winterwell.nlp.simpleparser.Parsers.chain;
import static com.winterwell.nlp.simpleparser.Parsers.first;
import static com.winterwell.nlp.simpleparser.Parsers.ignore;
import static com.winterwell.nlp.simpleparser.Parsers.lit;
import static com.winterwell.nlp.simpleparser.Parsers.opt;
import static com.winterwell.nlp.simpleparser.Parsers.optSpace;
import static com.winterwell.nlp.simpleparser.Parsers.ref;
import static com.winterwell.nlp.simpleparser.Parsers.seq;
import static com.winterwell.nlp.simpleparser.Parsers.space;
import static com.winterwell.nlp.simpleparser.Parsers.word;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.winterwell.moneyscript.lang.Lang;
import com.winterwell.moneyscript.lang.num.LangNum;
import com.winterwell.moneyscript.lang.time.LangTime;
import com.winterwell.nlp.simpleparser.AST;
import com.winterwell.nlp.simpleparser.PP;
import com.winterwell.nlp.simpleparser.ParseFail;
import com.winterwell.nlp.simpleparser.ParseResult;
import com.winterwell.nlp.simpleparser.ParseState;
import com.winterwell.nlp.simpleparser.Parser;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.containers.Slice;

/**
 * Loose Inspiration: css selectors
 * 
 * @author daniel
 * @testedby LangCellSetTest
 */
public class LangCellSet {

	static Pattern kw = Pattern.compile("\\b("+StrUtils.join(Lang.keywords, "|")+")\\b", 
										Pattern.CASE_INSENSITIVE);

	private static final String CELLSET = "CellSet";
	/**
	 * e.g. "Alice"
	 */
	private static final String CELLSET_SINGLE_ROW = "CellSet1Row";
	
	/**
	 * A general cell-set -- can span several rows, e.g. "Alice,Bob"
	 */
	public static final Parser<CellSet> cellSet = ref(CELLSET);
	/**
	 * A cell set limited to a single row.
	 * E.g. "Alice from year 2" is OK, but "Alice, Bob" will fail.
	 */
	public static final Parser<CellSet> cellSet1Row = ref(CELLSET_SINGLE_ROW);

	public static final String ROW_NAME = "row-name";
	
	/**
	 * NB: this can be shortened by {@link #kw}
	 */
	public static Pattern rowNameRegex = Pattern.compile(
//			"^[A-Z][^:,\\+\\-\\*\\/<>=%\\?\\(\\)#]*"
			"^[A-Z][A-Za-z0-9_ \\.]*"
			);	
	
	/**
	 * Parse row names, e.g. "Alice"
	 */
	public static Parser<RowName> rowName = new RowNameParser().label(ROW_NAME);
	
	static class RowNameParser extends Parser<RowName> {		
		
		@Override
		protected ParseResult<RowName> doParse(ParseState state) {			
			Slice unparsed = state.unparsed();
			Matcher m = rowNameRegex.matcher(unparsed);
			if ( ! m.find()) return null;			
			int end = m.end();			
			// keyword? e.g. "from" -- shorten what we parsed if it contains a keyword			
			String rn = state.unparsed().subSequence(0, end).toString();
			Matcher kwm = kw.matcher(rn);
			if (kwm.find()) {
				end = kwm.start();
				rn = rn.substring(0, end).trim();
				end = rn.length();
			}
			// trim end
			while(Character.isWhitespace(unparsed.charAt(end-1))) {
				end--;
				assert end !=0;
			}			
			assert end !=0; // return null; should be impossible due to commands lowercase, names Capitalised
			Slice parsedSlice = new Slice(unparsed, 0, end);
			// HACK: avoid months "Jan" is not a valid row
			Matcher mm = LangTime.MONTHYEAR_PARSER.regex.matcher(parsedSlice.toString());
			if (mm.find() && mm.start()==0) {
				return null;
			}
			// return
			RowName cs = new RowName(parsedSlice.toString());
			AST<RowName> ast = new AST<>(this, parsedSlice);
			ast.setX(cs);
			ParseResult<RowName> r = new ParseResult<>(state, ast, state.text, state.posn + end);
			return r;
		}
	}
	
	
	Parser<CellSet> all = new PP<CellSet>(word("all")) {
		@Override
		protected CellSet process(ParseResult<?> r) throws ParseFail {
			return new AllCellSet(r.parsed());
		}		
	};
	
	Parser<CellSet> thisRow = new PP<CellSet>(word("this row")) {
		@Override
		protected CellSet process(ParseResult<?> r) throws ParseFail {
			return new CurrentRow(r.parsed());
		}		
	};
	
	Parser<CellSet> rowSplit = new PP<CellSet>(seq(
			lit("split by"), space, first(rowName, LangNum.hashTag)
	)) {
		protected CellSet process(ParseResult<?> pr) {
			Object splitter = Containers.last(pr.getLeafValues());
			// NB: this will be replaced by a version with a base
			RowSplitCellSet cs = new RowSplitCellSet(null, (RowName) splitter, pr.parsed());
			return cs;
		}
	};

	Parser<SetVariable> variableFilter = new PP<SetVariable>(seq(
			rowName, ignore("="), rowName
	)) {
		protected SetVariable process(ParseResult<?> pr) {
			List rn_rn = pr.getLeafValues();
			RowName a = (RowName) rn_rn.get(0);
			RowName b = (RowName) rn_rn.get(1);
			return new SetVariable(a.getRowName(), b.getRowName());
		}
	};
	
	Parser<CellSet> rowNameWithFixedVariable = new PP<CellSet>(seq(
			rowName, optSpace, ignore("["), 
			chain(variableFilter, ignore(", ",",")), 
			ignore("]")
	)) {
		protected CellSet process(ParseResult<?> pr) {
			AST<RowName> rn = pr.getNode(rowName);
			List vs = Containers.filterByClass(pr.getLeafValues(), SetVariable.class);
			RowNameWithFixedVariables rnv = new RowNameWithFixedVariables(pr.parsed(), rn.getX().getRowName(), vs);
			return rnv;
		}
	};
	
	
	Parser<CellSet> selector1 = new PP<CellSet>(seq(
			first(rowNameWithFixedVariable, rowName, all, thisRow), optSpace, opt(first(LangFilter.filter, rowSplit))
	)) {
		protected CellSet process(ParseResult<?> pr) {
			List<AST> ls = pr.getLeaves();
			AST rowParsed = ls.get(0);
			CellSet base;
			if (rowParsed.getX() instanceof CellSet) {
				base = (CellSet) rowParsed.getX();
			} else {
				String rn = rowParsed.parsed(); // does this ever happen??
				base = new RowName(rn);							
			}
			// unfiltered?
			if (ls.size() == 1) {
				return base;
			}
			assert ls.size() == 2;
			Object f = ls.get(1).getX();
			if (f instanceof Filter) {
				return new FilteredCellSet(base, (Filter) f, pr.parsed());
			}
			RowSplitCellSet cs0 = (RowSplitCellSet) f;
			RowSplitCellSet cs = new RowSplitCellSet(base, cs0.splitBy, pr.parsed());
			return cs;
		}
	}.label(CELLSET_SINGLE_ROW);
		

	public static Parser<CellSet> cellSetFilter = new PP<CellSet>(LangFilter.filter) {
		@Override
		protected CellSet process(ParseResult<?> r) throws ParseFail {
			Object filter = r.getX();	
			AllCellSet base = new AllCellSet(null);
			return new FilteredCellSet(base, (Filter) filter, r.parsed());
		}		
	}.label("CellSetFilter");
	
	Parser<CellSet> listOfSetsSelector = new PP<CellSet>(
			chain(bracketed("(",selector1,")"), seq(lit(","), optSpace).label(null))
			) {
		@Override
		protected CellSet process(ParseResult r) {
			List<AST> ab = (List) r.ast.getLeaves();
			if (ab.size() == 1) {
				return (CellSet) ab.get(0).getX();
			}
			AST a = (AST) ab.get(0);
			AST b = (AST) ab.get(1);
			CellSet ax = (CellSet) a.getX();					
			CellSet bx = (CellSet) b.getX();
			return new Union(ax, bx, r.parsed());
		}			
	}.label(CELLSET);
}

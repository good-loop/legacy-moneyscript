package com.winterwell.moneyscript.lang.time;

import static com.winterwell.nlp.simpleparser.Parsers.*;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.MatchResult;

import com.winterwell.depot.IInit;
import com.winterwell.moneyscript.lang.cells.CellSet;
import com.winterwell.moneyscript.lang.cells.LangCellSet;
import com.winterwell.moneyscript.lang.num.Formula;
import com.winterwell.moneyscript.lang.num.LangNum;
import com.winterwell.moneyscript.lang.time.LangTimeTest;
import com.winterwell.nlp.simpleparser.AST;
import com.winterwell.nlp.simpleparser.PP;
import com.winterwell.nlp.simpleparser.ParseFail;
import com.winterwell.nlp.simpleparser.ParseResult;
import com.winterwell.nlp.simpleparser.Parser;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;

/**
 * @testedby {@link LangTimeTest}
 * @author daniel
 *
 */
public class LangTime implements IInit {

	public static final String from = "from";



	/**
	 * TODO uses Dt to allow for "quarter"
	 */
	private static final PP<TUnit> tunit = new PP<TUnit>(
			regex("(year|quarter|month|week|day|hour|minute)s?")) {
		protected TUnit process(ParseResult<?> pr) {
			MatchResult x = (MatchResult) pr.ast.getX();
			String us = x.group(1).toUpperCase();
			if ("QUARTER".equals(us)) {
				throw new ParseFail(pr, "Sorry - quarter is not supported yet");
			}
			TUnit u = TUnit.valueOf(us);
			if (u.getMillisecs() < TUnit.WEEK.getMillisecs()) {				
				throw new ParseFail(pr, "We only support year, quarter, month, or week.");
			}
			return u;
		}
	}.label("tunit");



	private static final String TIME = "time";
	private static final String DT = "dt";
	
	/**
	 * a point in time
	 */
	public static final Parser<TimeDesc> time = ref(TIME);
	/**
	 * a period of time
	 */
	public static final Parser<DtDesc> dt = ref(DT);

	/**
	 * "2 months", "year"
	 */
	final Parser<DtDesc> _dt = new PP<DtDesc>(first(
			seq(LangNum.num, space, tunit), 
			tunit)) 
	{
		@Override
		protected DtDesc process(ParseResult<?> r) {
			List<AST> ls = r.getLeaves();
			if (ls.size() == 1) {
				TUnit tu = (TUnit) ls.get(0).getX();
				return new DtDesc(tu.dt);
			}	
			Formula n = (Formula) ls.get(0).getX();
			TUnit tu = (TUnit) ls.get(1).getX();			
			return new DtDesc(n, tu);
		}		
	}.label(DT);
		


	/**
	 * NB: This is "from" for relative descriptions of a specific time. 
	 * "from" for _periods_ is defined by LangFilter
	 */
	final Parser<TimeDesc> complexTime = new PP<TimeDesc>(
			first(
					seq(dt, space, lit(from), space, ref(TIME)),
					seq(dt, space, lit("ago")))
	) {
		@Override
		protected TimeDesc process(ParseResult<?> r) throws ParseFail {
			List<AST> ls = r.getLeaves();
			DtDesc _dt = r.getNode(dt).getX();
			if (ls.size() == 2) {
				assert ls.get(1).parsed().equals("ago") : ls+" from "+r;
				return new TimeDesc(_dt, "ago", null);
			}
			TimeDesc td = (TimeDesc) ls.get(2).getX();
			return new TimeDesc(_dt, from, td);
		}	
	}.eg("2 months ago").eg("3 months from start");

	/**
	 * month year
	 */
	final Parser<TimeDesc> date = new PP<TimeDesc>(
			regex("(?i)(january|jan|february|febuary|feb|march|mar|april|apr|may|june|jun|july|jul|august|aug|september|sept|sep|october|oct|november|nov|december|dec)(\\s+(20\\d\\d))?")
	) {
		@Override
		protected TimeDesc process(ParseResult<?> r) throws ParseFail {
			String month = r.parsed().substring(0, 3).toLowerCase();
			MatchResult mr = (MatchResult) r.getX();
			String lastGrp = mr.group(mr.groupCount()-1);
			int yr = lastGrp != null && mr.groupCount()> 1? 
						Integer.valueOf(lastGrp.trim()) 
						: new Time().getYear();
			int i = Arrays.asList("jan","feb","mar","apr","may","jun","jul","aug","sep","oct","nov","dec").indexOf(month);
			assert i != -1 : r;	
			Time t = new Time(yr, i+1, 1);
			// TODO "from April" in December means from April of the next year
			return new SpecificTimeDesc(t);
		}	
	};

	/**
	 * "2019" = 1st January 2019
	 * HACK only parses 20XX dates!
	 */
	final Parser<TimeDesc> justYear = new PP<TimeDesc>(
			regex("20\\d\\d")
	) {
		@Override
		protected TimeDesc process(ParseResult<?> r) throws ParseFail {
			int yr = Integer.valueOf(r.parsed());
			Time t = new Time(yr, 1, 1);
			return new SpecificTimeDesc(t, r.parsed());
		}	
	};

	/**
	 * A point in time, e.g. "date" "now", "year 2"
	 * Also allow wrapping brackets, e.g. "(2 months ago)"
	 */
	final Parser<TimeDesc> _time = new PP<TimeDesc>(
			bracketed("(", first(
					seq(lit("start"), space, lit("of").label("of"), space, ref(LangCellSet.ROW_NAME)),
					lit("date", "now", "start", "previous", "quarter"),
					date,
					seq(tunit, space, num("n")),					
					complexTime,
					justYear
					), ")")
	) {
		protected TimeDesc process(ParseResult<?> r) {
			List<AST> ls = r.getLeaves();
			// "month 3"
			AST<TUnit> unit = r.getNode(tunit);
			if (unit!=null) {
				Number n = (Number)r.getNode("n").getX();
				TUnit _unit = unit.getX();
				double dn = n.doubleValue();
				return new TimeDesc(_unit, dn);	
			}
			// "3 months from now"
			AST<TimeDesc> ctn = r.getNode(complexTime);
			if (ctn!=null) return ctn.getX();
			// "start of US Sales"
			if (r.getNode("of")!=null) {
				CellSet row = (CellSet) r.getNode(LangCellSet.ROW_NAME).getX();
				TimeDesc start = new TimeDesc("start");
				start.setContext(row);
				return start;
			}			
			// just unwrap?
			if (ls.size()==1) {
				Object td = ls.get(0).getX();
				if (td instanceof TimeDesc) {
					return (TimeDesc) td;				
				}
			}
			// "previous" - handled later
			return new TimeDesc(r.parsed());
		};
	}.label(TIME);

	@Override
	public void init() {
		// TODO label mappings??
	}

	
}

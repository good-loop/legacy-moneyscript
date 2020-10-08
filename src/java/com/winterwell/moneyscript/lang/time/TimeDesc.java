package com.winterwell.moneyscript.lang.time;

import java.util.Collection;
import java.util.Set;
import java.util.logging.Level;

import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Col;
import com.winterwell.moneyscript.output.Row;
import com.winterwell.moneyscript.lang.cells.CellSet;
import com.winterwell.moneyscript.lang.num.Numerical;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;

/**
 * Describe one point in time (ie a column).
 * @author daniel
 *
 */
public class TimeDesc {

	private String desc;
//	private TUnit unit;
	private DtDesc dt;
//	private Number n;
	/**
	 * usually null. Non null in compound descriptions, such as
	 * "1 year from start"
	 */
	private TimeDesc base;
	private String op;
	/**
	 * Used for "start of X"
	 */
	private CellSet context;

	public TimeDesc(String parsed) {
		this.desc = parsed;
		assert desc != null;
		if ("quarter".equalsIgnoreCase(parsed)) {
			this.dt = new DtDesc(new Dt(3, TUnit.MONTH));
		}
	}

	public TimeDesc(TUnit unit, double n) {
		this(new DtDesc(new Dt(n, unit)), null, null);
	}

	/**
	 * E.g. "1 year from start", "2 months ago"
	 * @param dt
	 * @param op
	 * @param td null if op==ago
	 */
	public TimeDesc(DtDesc dt, String op, TimeDesc td) {
		assert op==null || LangTime.from.equals(op) || "ago".equals(op) : op;
		this.op = op;
//		this.unit = dt.getUnit();
//		n = dt.getValue();
		this.dt = dt;
		base = td;
	}

	@Override
	public String toString() {
		return (desc!=null? desc : dt.toString());
	}

	/**
	 * @return The (first) column for this time desc.
	 * Can be Col.THE_INDEFINITE_FUTURE or Col.THE_PAST. never null.
	 */
	public Col getCol(Cell context) {
		final Col focusCol = context==null? null : context.getColumn();
		if ("now".equals(desc) || "date".equals(desc)) {
			return focusCol;
		}
//		if ("previous".equals(desc)) {
//			Col now = Context.getColumn();
//			return now.index==1? Col.THE_INDEFINITE_FUTURE : new Col(now.index - 1);
//		}
		Dt _dt = dt==null? null : dt.calculate(context);
		
		if (op != null && op.equals("ago")) {					
			int n = (int) _dt.divide(context.getBusiness().getTimeStep());
			if (n>=focusCol.index) {
				return Col.THE_PAST;
			}
			return new Col(focusCol.index - n);
		}
		
		// "1 year from start"
		if (base != null) {
			assert op.equals(LangTime.from) : this;
			Col baseCol = base.getCol(context);
			if (_dt.getUnit() == TUnit.MONTH) {
				return new Col((int) (baseCol.index + _dt.getValue()));
			}
			if (_dt.getUnit() == TUnit.YEAR) {
				return new Col((int) (baseCol.index + _dt.getValue()*12));
			}
			throw new TodoException(dt.toString());	
		}
		
		// "month 2"
		if (dt != null) {
			if (_dt.getUnit() == TUnit.MONTH) {
				return new Col((int) _dt.getValue());
			}
			if (_dt.getUnit() == TUnit.YEAR) {
				// subtract 1 to go from 1 index to zero index, ie. year 2 = month 13
				int nz = (int) (_dt.getValue()-1);
				return new Col(1 + 12*nz);
			}
			throw new TodoException(dt.toString());	
		}
		
		// start = first non-zero cell in this row
		if ("start".equals(desc)) {		
			return getCol2_start(context);
		}		
		throw new TodoException(desc);
	}

	private Col getCol2_start(Cell focus) {
		// which row?
		Row row = focus.getRow();
		Business business = focus.getBusiness();
		if (context != null) {
			Set<String> rowNames = context.getRowNames();
			assert rowNames.size() == 1;
			String rn = Containers.first(rowNames);
			row = business.getRow(rn);
		}
		// find first non-zero col
		assert row != null;
		int focusColIndex = (focus.getColumn()==null? Col.THE_INDEFINITE_FUTURE : focus.getColumn()).index;
		Collection<Cell> cells = row.getCells();
		for(Cell cell : cells) {
			// if start is after the current focus col, then "fail" 
			if (cell.col.index > focusColIndex) {
				break;
			}
			Numerical v = business.getCellValue(cell);
			if (v==null) continue;
			if (v==Business.EVALUATING) {
				// This is a tricky case, but I think this is the right behaviour
				// COnsider: Alice from month 2: £1, Alice from start: *150%
//					Log.report("Ignoring EVALUATING in start of "+row, Level.FINE);
				continue;
			}
			if (v.doubleValue() != 0) {					
				return cell.col;
			}
		}
		// there is no start (yet)
		return Col.THE_INDEFINITE_FUTURE;
	}

	public void setContext(CellSet row) {
		this.context = row;
	}

	// add a context input??
	public Time getTime() {
		throw new TodoException(this);
	}

}

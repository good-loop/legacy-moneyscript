package com.winterwell.moneyscript.lang.cells;

import com.winterwell.moneyscript.lang.time.LangTime;
import com.winterwell.moneyscript.lang.time.SpecificTimeDesc;
import com.winterwell.moneyscript.lang.time.TimeDesc;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Col;

/**
 * to/from/at a point in time
 * @author daniel
 *
 */
public class TimeFilter extends Filter {
	
	private final TimeDesc time;

	public TimeFilter(String op, TimeDesc time) {
		assert time != null;
		this.op = op;
		assert op!=null && time != null;
		this.time = time;
		this.dirn = op.equals("to")? KDirn.LEFT : "at".equals(op)? KDirn.HERE : KDirn.RIGHT;
		assert op.equals("to") || op.equals(LangTime.from) || op.equals("at") : op;
	}

	@Override
	public boolean contains(Cell cell, Cell context) {
		assert cell.col != null : "no column for "+cell+" vs "+this;
		Col point = time.getCol(context);
		assert point !=null : time+" "+context;
		int pointIndex = point.index;
		// to time?
		if (dirn == KDirn.LEFT) {
			return cell.col.index <= pointIndex;
		}
		// in time?
		if (dirn == KDirn.HERE) {
			if (cell.col.index == pointIndex) return true;
			if (time instanceof SpecificTimeDesc) { // HACK in a quarter?
				int len = ((SpecificTimeDesc)time).getMonths();
				if (len > 1) {
					int endPointIndex = pointIndex + len;
					if (cell.col.index > pointIndex && cell.col.index < endPointIndex) {
						return true;
					}
				}
			}
			return false;
		}
		// from time?
		assert dirn == KDirn.RIGHT : dirn;
		return cell.col.index >= pointIndex;
	}
	
	@Override
	public String toString() {
		return dirn+" "+time;
	}

}

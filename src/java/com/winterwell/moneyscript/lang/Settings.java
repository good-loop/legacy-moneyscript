package com.winterwell.moneyscript.lang;

import java.lang.reflect.Field;

import com.goodloop.data.KCurrency;
import com.winterwell.maths.timeseries.TimeSlicer;
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;

public final class Settings {

	private Time _end;
	private Time _start;
	public Dt timeStep = TUnit.MONTH.dt;
	/**
	 * Not sure what this should apply to :(
	 */
	String css;
	/**
	 * 1 or 0 mean "don't sample"
	 */
	int samples;
	protected Dt _runTime;
		
	/**
	 * TODO use to switch $ handling on/off
	 */
	KCurrency currency;
	
	KNumberFormat numberFormat;
	
	public void setSamples(int samples) {
		this.samples = samples;
	}
	/**
	 * 
	 * @param s2 NB: will over-write anything in this
	 * @return copy with merge
	 */
	public Settings merge(Settings s2) {
		Settings s3 = new Settings();
		try {
			for(Field f : ReflectionUtils.getAllFields(getClass())) {
				Object a = f.get(this);
				Object b = f.get(s2);
				Object dflt = f.get(s3);
				Object c = null;
				if (a!=dflt) c = a;
				if (b!=dflt) {
//					assert c==null || c.equals(b) : c+ " vs "+b; allow over-write
					c = b;
				}
				if (c == null) continue;
				f.set(s3, c);	
			}
		} catch (Exception e) {
			throw Utils.runtime(e);
		}
//		s3.start = Utils.or(start, s2.start);
		return s3;
	}

	private Time round(Time time) {
		return new Time(time.getYear(), time.getMonth(), time.getDayOfMonth());
	}

	public Time getEnd() {
		if (_end==null) {
			return getStart().plus(getRunTime()); 
		}
		return _end;
	}
	
	public Dt getRunTime() {
		if (_start!=null && _end != null) {
			return _start.diff(_end, TUnit.MONTH);
		}
		
		if (_runTime != null) return _runTime;
		
		return new Dt(2, TUnit.YEAR);
	}

	public Time getStart() {
		if (_start==null) {
			return round(new Time()); 
		}
		return _start;
	}

	@Override
	public String toString() {
		return "Settings[_end=" + _end + ", _start=" + _start + "]";
	}
	public int getSamples() {
		return samples;
	}
	public void setStart(Time time) {
		this._start = time;
		ts = null;
	}
	/**
	 * Also set the year-end!
	 * @param _end
	 */
	public void setEnd(Time _end) {
		this._end = _end;
		ts = null;		
	}	
	
	public void setYearEnd(Integer yearEnd) {
		this.yearEnd = yearEnd;
	}

	private transient TimeSlicer ts;

	public TimeSlicer getTimeSlicer() {
		if (ts==null) {
			ts = new TimeSlicer(getStart(), getEnd(), timeStep);
		}
		return ts;
	}
	
	/**
	 * 12=dec (default), 3=march
	 */
	private Integer yearEnd;
	/**
	 * HACK hide to: Jan 2021 means calculate but dont display the earlier columns
	 */
	Time hideTo;
	
	/**
	 * If unset, use end. If that is unset default to December
	 * 12=dec (default), 3=march
	 */
	public int getYearEnd() {
		if (yearEnd==null) {
			if (_end==null) {
				return 12;
			}
			return _end.getMonth();
		}
		return yearEnd;
	}
	public KNumberFormat getNumberFormat() {
		if (numberFormat==null) numberFormat = KNumberFormat.abbreviate;
		return numberFormat;
	}
	public void setHideTo(Time time) {
		this.hideTo = time;
	}
}

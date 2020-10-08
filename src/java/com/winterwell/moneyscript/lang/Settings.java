package com.winterwell.moneyscript.lang;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.GregorianCalendar;

import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;

public class Settings {

	public Time _end;
	public Time _start;
	public Dt timeStep = TUnit.MONTH.dt;
	/**
	 * Not sure what this should apply to :(
	 */
	String css;
	int samples;
	protected Dt _runTime;
	
	public void setSamples(int samples) {
		this.samples = samples;
	}
	/**
	 * 
	 * @param s2 This will over-write
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

	
	
}

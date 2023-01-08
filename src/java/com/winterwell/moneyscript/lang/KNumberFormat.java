package com.winterwell.moneyscript.lang;

import com.winterwell.moneyscript.lang.num.Numerical;

public enum KNumberFormat {
	/** this is our default */
	abbreviate,
	standard,
	plain;

	public String str(Numerical v) {
		if (v==null) return "";
		switch(this) {
		case abbreviate:
			return v.toString();
		case standard:
			return v.toExportString(true);
		case plain:
			return v.toExportString(false);
		}
		return v.toString();
	}
}

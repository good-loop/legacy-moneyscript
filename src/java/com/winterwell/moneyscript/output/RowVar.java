package com.winterwell.moneyscript.output;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class RowVar implements Comparable<RowVar> {
	public RowVar(String name) {
		this.name = name;
	}
	final String name;
	/**
	 * "enum" of values, e.g. Region:[UK,US,EU]
	 */
	final List<String> values = new ArrayList();
	/**
	 * Current active value
	 */
	String value;
	
	void addValue(String val) {
		if (values.contains(val)) return;
		values.add(val);
	}
	
	public String setValue(String val) {
		var old = value;
		value = val;
		return old;
	}

	@Override
	public String toString() {
		return "Var [name=" + name + ", values=" + values + ", value=" + value + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RowVar other = (RowVar) obj;
		return Objects.equals(name, other.name) && Objects.equals(value, other.value);
	}

	@Override
	public int compareTo(RowVar o) {
		return name.compareTo(o.name);
	}
	
}
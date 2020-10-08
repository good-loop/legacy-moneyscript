package com.winterwell.moneyscript.output;

/**
 * This is a reference -- NOT the cell value itself
 * @author daniel
 *
 */
public final class Cell {

	/**
	 * Can be null
	 */
	public final Row row;

	/**
	 * Can be null
	 */
	public final Col col;
	
	@Override
	public String toString() {
		return "["+row+", "+col+"]";
	}
	
	public Cell(Row row, Col col) {
		this.row = row;
		this.col = col;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((col == null) ? 0 : col.hashCode());
		result = prime * result + ((row == null) ? 0 : row.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Cell other = (Cell) obj;
		if (col == null) {
			if (other.col != null)
				return false;
		} else if (!col.equals(other.col))
			return false;
		if (row == null) {
			if (other.row != null)
				return false;
		} else if (!row.equals(other.row))
			return false;
		return true;
	}

	public static Business getBusiness() {
		return Business.get();
	}

	public Row getRow() {
		return row;
	}
	
	public Col getColumn() {
		return col;
	}
	
}

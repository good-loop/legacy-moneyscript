package com.winterwell.moneyscript.lang.num;



/**
 * For 10 widgets @ £5 - which is £50, but the 10 is still accessible via #
 * @author daniel
 *
 */
public class Numerical2 extends Numerical {
	private static final long serialVersionUID = 1L;
	private Numerical lhs;

	public Numerical2(Numerical n, Numerical lhs) {
		super(n.doubleValue(), n.getUnit());
		assert n.getClass() == Numerical.class : n; // no nesting, or unsampled uncertains
		this.lhs = lhs;
	}
	
	public Numerical getLhs() {
		return lhs;
	}
}
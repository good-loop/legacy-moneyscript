package com.winterwell.moneyscript.lang;

public interface IReset {

	/**
	 * Get ready for fresh use. If it has already been used, discard all learned
	 * state (but *not* any configuration settings). I.e. this is both init
	 * and reset.
	 */
	void reset();
	
}

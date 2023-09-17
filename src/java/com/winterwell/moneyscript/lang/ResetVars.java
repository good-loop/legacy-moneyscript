package com.winterwell.moneyscript.lang;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import com.winterwell.moneyscript.output.Row;
import com.winterwell.moneyscript.output.VarSystem;
import com.winterwell.utils.containers.Pair;
import com.winterwell.utils.containers.Pair2;

public class ResetVars implements Closeable {

	private VarSystem vars;
	private List<Pair<String>> resets;

	public ResetVars(VarSystem vars, List<Pair<String>> resets) {
		this.vars = vars;
		this.resets = resets;
	}

	@Override
	public void close() throws IOException {
		for(Pair<String> p : resets) {
			vars.setRow4Name(p.first, p.second);
		}
	}

	@Override
	public String toString() {
		return "ResetVars[" + resets + "]";
	}

}

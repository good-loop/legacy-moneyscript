package com.winterwell.moneyscript.webapp;

import java.lang.reflect.Constructor;

import com.winterwell.moneyscript.lang.cells.Scenario;
import com.winterwell.utils.AString;
import com.winterwell.utils.Key;
import com.winterwell.utils.Utils;
import com.winterwell.utils.io.ISerialize;
import com.winterwell.web.fields.AField;

public class AStringField<S extends AString> extends AField<S> {
	
	private Constructor<S> cons;

	public AStringField(String name, Class<S> class1) {
		super(name);
		try {
			this.cons = class1.getConstructor(String.class);
		} catch (NoSuchMethodException | SecurityException e) {
			throw Utils.runtime(e);
		}
	}

	@Override
	public S fromString(String v) throws Exception {
		return cons.newInstance(v);
	}

}

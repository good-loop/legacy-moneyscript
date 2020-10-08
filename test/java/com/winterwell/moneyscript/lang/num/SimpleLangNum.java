package com.winterwell.moneyscript.lang.num;

import com.winterwell.nlp.simpleparser.PP;
import com.winterwell.nlp.simpleparser.ParseFail;
import com.winterwell.nlp.simpleparser.ParseResult;
import com.winterwell.nlp.simpleparser.Parsers;

/**
 * Like {@link LangNum} but it only recognises integers
 * @author daniel
 *
 */
public class SimpleLangNum {

	public SimpleLangNum() {
		new PP<Formula>(Parsers.regex("\\d+")) {
			@Override
			protected Formula process(ParseResult<?> r) throws ParseFail {
				return new BasicFormula(new Numerical(r.parsed()));
			}			
		}.label(LangNum.num.getName());
	}
	
}

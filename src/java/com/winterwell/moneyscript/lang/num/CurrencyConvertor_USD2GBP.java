package com.winterwell.moneyscript.lang.num;

import com.goodloop.data.KCurrency;
import com.winterwell.datalog.server.CurrencyConvertor;
import com.winterwell.utils.time.Time;

/**
 * HACK wrap currency convertor for USD to GBP
 * @author daniel
 *
 */
public class CurrencyConvertor_USD2GBP extends CurrencyConvertor {

	public CurrencyConvertor_USD2GBP(Time date) {
		super(KCurrency.USD, KCurrency.GBP, date);
	}

}

package com.winterwell.moneyscript.webapp;

import com.goodloop.data.KCurrency;
import com.winterwell.utils.io.Option;
import com.winterwell.web.app.ISiteConfig;

public class MoneyScriptConfig implements ISiteConfig {

	/**
	 * If set, convert $s to £s @deprecated move to PlanDoc
	 */
	@Option
	KCurrency currency;
	
	private int port = 8722;

	@Override
	public int getPort() {
		return port;
	}

}

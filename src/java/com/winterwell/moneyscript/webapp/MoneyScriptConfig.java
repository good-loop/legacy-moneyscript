package com.winterwell.moneyscript.webapp;

import com.winterwell.web.app.ISiteConfig;

public class MoneyScriptConfig implements ISiteConfig {

	private int port = 8722;

	@Override
	public int getPort() {
		return port;
	}

}

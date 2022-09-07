package com.goodloop.xero;

import java.io.File;
import java.util.Map;

import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.FakeBrowser;
import com.winterwell.web.app.IServlet;
import com.winterwell.web.app.WebRequest;

/**
 * Listen for codes and tokens from Xero
 * @author daniel
 *
 */
class XeroCodeServlet implements IServlet {

	private XeroConfig xsc;
	private File tokenFile;

	public XeroCodeServlet(XeroConfig xsc, File tokenFile) {
		this.xsc = xsc;
		this.tokenFile = tokenFile;
	}

	@Override
	public void process(WebRequest state) throws Exception {
		String code = state.get("code");
		assert code != null;
		String exchange_code_url = "https://identity.xero.com/connect/token";
		FakeBrowser fb = new FakeBrowser();
		fb.setAuthentication(xsc.client_id, xsc.client_secret);
		String redirect_uri = 
				"http://localhost:"+state.getRequest().getLocalPort()+"/xeroResponse";
		String response = fb.post(exchange_code_url, new ArrayMap(
                "grant_type", "authorization_code",
                "code", code,
                "redirect_uri", redirect_uri
		));
		// Save the token
		Log.i("auth", "Save token from authorization_code: "+response);
		Map token = (Map) WebUtils2.parseJSON(response);
		xsc.token = token;
		FileUtils.write(tokenFile, response);
	}
	
}
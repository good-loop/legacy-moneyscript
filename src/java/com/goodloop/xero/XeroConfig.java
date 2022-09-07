package com.goodloop.xero;

import java.io.File;
import java.util.Map;

import com.winterwell.gson.Gson;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.FakeBrowser;

class XeroConfig {
	
	String tenantId;

	volatile Map token;

	String client_id;
	
	
	String client_secret;
	
	
	String refresh_token;
	

	static void refreshToken(XeroConfig xsc, File xscFile, Gson gson) {
		FakeBrowser fb = new FakeBrowser();
		fb.setAuthentication(xsc.client_id, xsc.client_secret);
		fb.setRequestHeader("Accept","application/x-www-form-urlencoded");
		String token_refresh_url = "https://identity.xero.com/connect/token";
		String refresh_token = (String) xsc.token.get("refresh_token");
		String response = fb.post(token_refresh_url, new ArrayMap(
                "grant_type", "refresh_token",
                "refresh_token", refresh_token
		));
		// Save new access token
		Map token = WebUtils2.parseJSON(response);
		xsc.token = token;
		FileUtils.write(xscFile, gson.toJson(xsc));
	}

}
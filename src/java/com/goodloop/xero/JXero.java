package com.goodloop.xero;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.goodloop.xero.data.Invoice;
import com.winterwell.gson.Gson;
import com.winterwell.gson.GsonBuilder;
import com.winterwell.gson.JsonArray;
import com.winterwell.gson.JsonElement;
import com.winterwell.gson.JsonObject;
import com.winterwell.gson.JsonParser;
import com.winterwell.maths.datastorage.DataTable;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeParser;
import com.winterwell.utils.time.TimeUtils;
import com.winterwell.utils.web.SimpleJson;
import com.winterwell.utils.web.WebUtils;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.ConfigException;
import com.winterwell.web.FakeBrowser;
import com.winterwell.web.app.JettyLauncher;
import com.winterwell.web.app.Logins;
import com.winterwell.web.app.MasterServlet;

/**
 * 
 * What an ugly API Xero has! Oh well, let's clean it up a bit for them.
 * 
 * See https://developer.xero.com/documentation/api/reports
 * 
 * ### First Time Authentication

You will need to do the following steps if you are connecting to Xero's API for the **first time**. 
For subsequent access, the following authentication steps are not needed anymore.

1. Sign in to your Xero account. Go to [Xero Apps](https://developer.xero.com/myapps).
2. On the top right, click on `New App`.
3. Fill in the corresponding fields:

	App name: Good-Loop
	Company url: https://good-loop.com
	Oauth 2.0 credentials
	OAuth 2.0 redirect URIs: http://localhost:7561/xeroResponse

4. Once you are done, click on `Create App`.
5. Scroll down and you'll see a `Client ID` field. 
	Create a file `client_id.txt` located in this directory and save the id into the file.
6. Click on 'generate secret' and you'll see a `Client Secret` field. 
	Create a file `client_secret.txt` located in this directory and save the secret into the file.  

 * @author daniel
 *
 */
public class JXero {
	
	private static final String LOGTAG = "JXero";

	public JXero() {
		
	}
	
	XeroConfig xsc;
	private Gson gson;
	
	public void init() {
		// Read in the auth details
		xsc = new XeroConfig();
		File xscFile = Logins.getLoginFile("xero", "xeroconfig.json");
		Gson gson = new Gson();
		if (xscFile.isFile()) {
			try {
				xsc = gson.fromJson(FileUtils.read(xscFile), XeroConfig.class);
			} catch(Exception ex) {
				Log.e(LOGTAG, ex);
				// An error with the config file? swallow and redo the auth 
			}
		} else {
			Log.d(LOGTAG, "No "+xscFile);
		}
		// The access token expires after 30 minutes
		// To remove the need of authenticating for subsequent access, we use the refresh token if there is one
		if (xsc.token==null) {
			setupToken(xsc, xscFile, gson);
		} else { // user refresh token to get a new access token
			XeroConfig.refreshToken(xsc, xscFile, gson);
		}
		
		// Hurrah we have a token!
		
		// Fetch the tenants (the ID is needed for getting data)
		FakeBrowser fb = new FakeBrowser();
		String accessToken = (String) xsc.token.get("access_token");
		assert accessToken!=null : xsc.token;
		fb.setAuthenticationByJWT(accessToken);
		fb.setRequestHeader("Content-Type", "application/json");
	    String connections_url = "https://api.xero.com/connections";
	    String response = fb.getPage(connections_url);
	    List<Map> tenants = Containers.asList((Object)WebUtils2.parseJSON(response));
		System.out.println(response);
		// make sure that the tenant is Good-Loop
		for (Map tenant: tenants) {
			if (tenant.get("tenantName").equals("Good-Loop Ltd")) {
				xsc.tenantId = (String) tenant.get("tenantId");
				break;
			}
		}
		assert xsc.tenantId!=null; 
	}
	

	/**
	 * Call Xero to get user authorisation -- and an access token
	 * (which will be saved to xscFile).
	 * @param xsc
	 * @param xscFile
	 * @param gson
	 */
	private static void setupToken(XeroConfig xsc, File xscFile, Gson gson) {
		// HACK
		File tokenFile = Logins.getLoginFile("xero", "xerotoken.txt");
		// HACK - deprecate in favour of just the json config
		// look in the special logins dir or locally
		if (xsc.client_id==null) {
			File clientIdFile = FileUtils.or(
					Logins.getLoginFile("xero", "client_id.txt"),
					new File("xero", "client_id.txt")
					);
			if (clientIdFile != null) {
				xsc.client_id = FileUtils.read(clientIdFile).trim();
			}
		}
		if (xsc.client_secret==null) {
			File clientSecretFile = FileUtils.or(
					Logins.getLoginFile("xero", "client_secret.txt"),
					new File("xero", "client_secret.txt")
					);
			if (clientSecretFile!=null) {
				xsc.client_secret = FileUtils.read(clientSecretFile).trim();
			}
		}		
		if (Utils.isBlank(xsc.client_id) || Utils.isBlank(xsc.client_secret)) {			
			throw new ConfigException("No client_id / client_secret-- Get from https://developer.xero.com/myapps and add to "+xscFile, "Xero");
		}
		
		// ...get ready to listen for a response
		JettyLauncher jl = new JettyLauncher(null, 7561);
		MasterServlet ms = jl.addMasterServlet();
		ms.addServlet("/xeroResponse", new XeroCodeServlet(xsc, tokenFile));
		jl.run();
		// ...ask for a code
		String redirect_uri = 
			"http://localhost:"+jl.getPort()+"/xeroResponse";
		String scope = "offline_access accounting.reports.read accounting.transactions payroll.payruns.read payroll.payslip.read";
		// NB: This method will url encode the id & secret. Is that needed? - well it can't do any harm.
		String url = WebUtils.addQueryParameters(
				"https://login.xero.com/identity/connect/authorize",
				new ArrayMap(
						"response_type","code",
						"client_id", xsc.client_id,
						"redirect_uri", redirect_uri,
						"scope", scope,
						"state","123" // a random number to prevent spoofing -- can be any number
				));		
		WebUtils2.browseOnDesktop(url);
		
		while(xsc.token==null) { // wait
			Utils.sleep(100);
		}
		// save the token
		FileUtils.write(xscFile, gson.toJson(xsc));
		jl.close();
	}
	
	
		
//		// Xero API has a bug where it could only handle the latest month that ends with 31st
//		if (end.getDayOfMonth() != 31) {
//			start = TimeUtils.getStartOfMonth(new Time());
//			end = TimeUtils.getEndOfMonth(new Time()).minus(TUnit.SECOND);
//		}
	

	/**
	 * see https://developer.xero.com/documentation/api/accounting/invoices
	 * @return
	 */
	public List<Invoice> fetchInvoices(Time modifiedSince) {
		// P&L API url
				
		String pnlUrl = "https://api.xero.com/api.xro/2.0/Invoices"
//				+"?page=1"
				;
		// NB: odd that it wants start, end, timeframe and number of periods -- there's some redundant info there
		// -- ah, it can and will roll several months into one report. We avoid that here.
		FakeBrowser fb = fetch4_fb();
		if (modifiedSince != null) {
			fb.setRequestHeader("If-Modified-Since", modifiedSince.toISOString());
		}
		String got = fb.getPage(pnlUrl);
		Map jobj = WebUtils2.parseJSON(got);
		List<Map> invoices = SimpleJson.getList(jobj, "Invoices");
		List is = Containers.apply(invoices, i -> gson().convert(i, Invoice.class));
		return is;
	}
	
	Gson gson() {
		if (gson==null) {
			GsonBuilder gb = new GsonBuilder();
			gson = gb.create();
		}
		return gson;
	}
	
	/**
	 * @param xsc
	 * @param start first day of latest month to fetch data from
	 * @param end last day of latest month to fetch data from
	 * @param timeframe e.g. TUnit.MONTH or TUnit.YEAR
	 * @return [headers, [row title (String), values (BigDecimal)...] ...] 
	 * Time runs left to right as normal (not right to left as Xero returns)
	 * Times are converted from Xero's  e.g. "30 Nov 20" to ISO format e.g. "2020-11-30"
	 */
	public DataTable<String> fetchProfitAndLoss(Time _start, Time _end, TUnit timeframe) {
		return fetch2("ProfitAndLoss", _start, _end, timeframe);
	}
	
	DataTable<String> fetch2(String report, Time _start, Time _end, TUnit timeframe) {
		if (_start.isAfter(_end)) throw new IllegalArgumentException("start after end");
		// HACK 1st jan? ignore Jan, stop at Dec
		if (_end.getMonth() == 1 && _end.getDayOfMonth() == 1) {
			_end = new Time(_end.getYear() - 1, 12, 31);
		}
		// whole year! This is to avoid the Xero-API-sometimes-returns-garbage bug (depending on the months), Dec 2021
		Time start = new Time(_start.getYear(), 1, 1);
		Time end = new Time(_end.getYear(), 12, 31);		
		assert timeframe==TUnit.MONTH : timeframe; // TODO year
		// if the period requested in more than a year, split into several API calls to Xero
		// eg: for a period of 2019-05-01 to 2021-03-31
		// we will split it into three calls which are 2021-01-01 to 2021-03-31, 2020-01-01 to 2020-12-31, 2019-05-01 to 2019-12-31
		
		DataTable<String> dt = new DataTable<>();
		Time end1 = end;
		while(end1.isAfter(start)) {
			Time start1 = end1.minus(TUnit.YEAR);
			// Get 1 year
			List<List<Object>> got1year = fetch3_1year(report, start1, end1, timeframe);
			// This will sort the columns into sensible order!
			DataTable<String> newData = new DataTable<>(got1year);
			dt = DataTable.merge(newData, dt);
			end1 = end1.minus(TUnit.YEAR);
		}
		
		// filter down to start/end
		int skipStart = 1 + _start.getMonth() - start.getMonth(); // NB: +1 to skip the title which is done separately
		int skipEnd = end.getMonth() - _end.getMonth();
		Dt dMonths = _start.diff(_end, TUnit.MONTH);
		// NB: the rounding removes an easy to have 1 day error due to start vs end of day 
		int nkeep = (int) Math.round(dMonths.getValue());
		List<Object[]> got2 = new ArrayList<>();
		for (Object[] row : dt) {
			if (row.length < skipStart+nkeep) {
				continue;
			}
			ArrayList row2 = new ArrayList();
			String rowTitle = (String) row[0];
			row2.add(rowTitle);
			Object[] rowData = Arrays.copyOfRange(row, skipStart, skipStart+nkeep);
			row2.addAll(Arrays.asList(rowData));
			got2.add(row2.toArray());
		}
		
		DataTable<String> dt2 = DataTable.fromRows(got2);
		return dt2;
	}


	private List<List<Object>> fetch3_1year(String report, Time start, Time end, TUnit timeframe) {
		// debug
		int n = (int) Math.round(start.diff(end, TUnit.MONTH).getValue());
		assert n > 0 : start+" is AFTER "+end;
		int numPeriods = Math.max(n - 1, 1); // NB: I don't know why, but asking for 11 months gets you a year of data
		assert numPeriods < 12 : "Too big - ask for a smaller chunk: "+n;
		String toDate = end.toISOStringDateOnly();
		// HACK we want start/end days for the last period
		Time s2 = end.minus(timeframe).plus(TUnit.DAY);
		// NB: +1 day keeps it within the same month/year as end -- which is what Xero demands
		Time eom = TimeUtils.getEndOfMonth(end).minus(TUnit.SECOND);
		if (timeframe==TUnit.MONTH 
			&& toDate.equals(eom.toISOStringDateOnly())
			) {
			s2 = TimeUtils.getStartOfMonth(end);
		}						
		String fromDate = s2.toISOStringDateOnly();
		// P&L API url
		String pnlUrl = "https://api.xero.com/api.xro/2.0/Reports/"+report+"?"
				+("BalanceSheet".equals(report)? 
						"date="+toDate
						: "fromDate="+ fromDate+ "&toDate=" + toDate)
				+ "&timeframe="+timeframe
				+"&periods="+numPeriods
				;
		// NB: odd that it wants start, end, timeframe and number of periods -- there's some redundant info there
		// -- ah, it can and will roll several months into one report. We avoid that here.
		List<List<Object>> got = fetch3(pnlUrl);
		return got;
	}

	/**
	 * TODO
	 */
	boolean timeDirnLeftToRight;

	/**
	 * @param xsc
	 * @param start first day of latest month to fetch data from
	 * @param end last day of latest month to fetch data from. 
	 * NB: This is the date of the "main" balance sheet returned - the other months are comparison periods
	 * @param timeframe e.g. TUnit.MONTH or TUnit.YEAR
	 * @return [headers, [row title (String), values (BigDecimal)...] ...] 
	 * Note: time runs right to left! Column 1 is the most recent month/year.
	 * Times are converted from Xero's  e.g. "30 Nov 20" to ISO format e.g. "2020-11-30"
	 */
	public DataTable<String> fetchBalanceSheet(Time start, Time end, TUnit timeframe) {	
		return fetch2("BalanceSheet", start, end, timeframe);		
	}
	

	private List<List<Object>> fetch3(String pnlUrl) {
		FakeBrowser fb = fetch4_fb();
		String response = fb.getPage(pnlUrl);
		
		// parse the json response
		List<List<Object>> content = new ArrayList();
		JsonElement jelement = new JsonParser().parse(response);
		JsonObject  jobject = jelement.getAsJsonObject();
		JsonArray jarray = jobject.getAsJsonArray("Reports");
		jobject = jarray.get(0).getAsJsonObject();
		jarray = jobject.getAsJsonArray("Rows");
		for (JsonElement row: jarray) {
			jobject = row.getAsJsonObject();
			// Header
			String rowType = jobject.get("RowType").getAsString();
			if (rowType.equals("Header")) {
				ArrayList<Object> headers = new ArrayList<Object>();
				boolean first = true;
				for (JsonElement cell: jobject.getAsJsonArray("Cells")) {
					String sv = svalue(cell);
					if ( ! first) {
						// convert the time formats to ISO standard
						TimeParser tp = new TimeParser();
						Time t = tp.parseExperimental(sv);
						sv = t.toISOStringDateOnly();
					}
					headers.add(sv);
					first = false;
				}
				content.add(headers);
				content.add(new ArrayList<Object>());
				continue;
			}
			// Section
			if (rowType.equals("Section")) {
				if ( ! jobject.get("Title").getAsString().isEmpty()) {
					String title =jobject.get("Title").getAsString();
					content.add(Arrays.asList(title));
				}
				for (JsonElement subrow: jobject.getAsJsonArray("Rows")) {
					ArrayList<Object> arr = new ArrayList<Object>();
					boolean first = true;
					for (JsonElement cell: subrow.getAsJsonObject().getAsJsonArray("Cells")) {
						Object v = first? svalue(cell) : num(cell);
						arr.add(v);
						first = false;
					}
					content.add(arr);
					if (subrow.getAsJsonObject().get("RowType").getAsString().equals("SummaryRow")) {
						content.add(new ArrayList<Object>());
					}
				}
				continue;
			}
			Log.e(LOGTAG, rowType);
		}
		
		return content;		
	}


	/**
	 * auth and get
	 * @param pnlUrl
	 * @return
	 */
	private FakeBrowser fetch4_fb() {
		FakeBrowser fb = new FakeBrowser();
		fb.setDebug(true);
		// auth
		if (xsc.bearer != null) {
			fb.setAuthenticationByJWT(xsc.bearer);
		} else {		
			String accessToken = (String) xsc.token.get("access_token");
			assert accessToken!=null : xsc.token;
			fb.setAuthenticationByJWT(accessToken);
		}
		fb.setRequestHeader("Xero-tenant-id", xsc.tenantId);
		fb.setRequestHeader("Accept","application/json");
		return fb;
	}


	private String svalue(JsonElement cell) {
		return cell.getAsJsonObject().get("Value").getAsString();
	}


	/**
	 * To avoid rounding errors in financial data, we return BigDecimal
	 * @param cell
	 * @return
	 */
	private BigDecimal num(JsonElement cell) {
		return cell.getAsJsonObject().get("Value").getAsBigDecimal();
	}


	/**
	 * TODO https://developer.xero.com/documentation/api/payrolluk/payruns
	 * 
	 * @deprecated doesn't work -- but you can export a big csv from the UI
	 * @param start
	 * @param end
	 * @return
	 */
	DataTable<String> fetchPayroll(Time start, Time end) {
		FakeBrowser fb = fetch4_fb();
		String got = fb.getPage("https://api.xero.com/payroll.xro/2.0/payRuns");
		System.out.println(got);
//		https://api.xero.com/payroll.xro/2.0/payRuns
		DataTable<String> fetched = fetch2("PayrollActivityDetails", start, end, TUnit.MONTH);
		return fetched;
	}

	
}

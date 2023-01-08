package com.winterwell.moneyscript.lang.time;

import org.junit.Test;

import com.winterwell.gson.Gson;
import com.winterwell.moneyscript.data.PlanDoc;
import com.winterwell.moneyscript.lang.Lang;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Col;
import com.winterwell.moneyscript.webapp.MoneyScriptMain;
import com.winterwell.utils.Dep;
import com.winterwell.utils.time.Time;
import com.winterwell.web.ajax.JThing;

public class TimeDescTest {

	@Test
	public void testGetCol() {
		Lang lang = new Lang();
		Business b = lang.parse("start: Jan 2022\nend: Mar 2022\nAlice: 1");
		SpecificTimeDesc sdt = new SpecificTimeDesc(new Time(2021,6,1), "Jun 2021");
		Cell cell = new Cell(b.getRow("Alice"), new Col(1));
		Col c = sdt.getCol(cell);
		assert c.index < cell.col.index : c;
	}

	@Test
	public void testWTF() {
		String sjosn = "{\"sheets\":[{\"@class\":\"com.winterwell.moneyscript.data.PlanSheet\",\"id\":\"fgtDnjlyUm\",\"text\":\"Hello: 1\\n\\n\",\"title\":\"Sheet 1\"}],\"@class\":\"com.winterwell.moneyscript.data.PlanDoc\",\"created\":\"2022-12-05T13:16:24Z\",\"importCommands\":[],\"exportCommands\":[{\"active\":true,\"from\":\"Jan 2020\"}],\"lastModified\":\"2022-12-05T13:16:24Z\",\"id\":\"T2XIihVo\",\"errors\":[],\"status\":\"DRAFT\"}";
		JThing jt = new JThing();
		jt.setJson(sjosn);
		jt.setType(PlanDoc.class);
		jt.java();
	}
	
	@Test
	public void testGsonBugDec22() throws NoSuchMethodException, SecurityException {
		String json = "{\"charts\":[{\"lines\":[]}],\"sheets\":[{\"@class\":\"com.winterwell.moneyscript.data.PlanSheet\",\"id\":\"3tAA6EvJ2c\",\"text\":\"Rent:\\n    Edinburgh Rent: £3000 per month #hq // a smaller-but-still-nice space \\n    //Edinburgh Rent from Oct 2023: £60k per year #hq // Sugar Bond office\\n    London Rent: £51k per year #hq // Somerset House\\n    London Business Rates: £17340 per year #hq\\n    London Buildings Insurance: £1823 per year #hq\\n    US Rent from Jan 2023: £700 * 2 #us\\n    Adhoc Rental Costs: £5k per year #hq\\n\\nPR Marketing: // Excluding staff. See detailed spending plan here https://docs.google.com/spreadsheets/d/15jesVE8lHePZVSNk66YL8gm_8PyJ6O8iyTtZyLGNdbI/edit#gid\\u003d1726297911\\n    Marketing from Jan 2022 to Dec 2022: £294,800 per year #marketing\\n    Marketing from Jan 2023 to Dec 2023: £300,000  per year #marketing // flat-line marketing spend y.o.y.\\n    Marketing from Jan 2024 to Dec 2024: £300,000 per year #marketing \\n    //US Marketing: £4k per month #us\\n    //PR: £300 per month #marketing\\n    //Events \\u0026 Sponsorship: 0 #marketing // counted in Marketing\\n    //Catering Goods: £50 #marketing\",\"title\":\"Sheet 1\"}],\"@class\":\"com.winterwell.moneyscript.data.PlanDoc\",\"created\":\"2022-12-05T11:55:36Z\",\"importCommands\":[],\"exportCommands\":[{\"from\":\"Feb 2020\"}],\"lastModified\":\"2022-12-05T11:55:45Z\",\"id\":\"Sk9WIqxZ\",\"errors\":[],\"status\":\"DRAFT\"}";
		MoneyScriptMain msm = new MoneyScriptMain();
		msm.init();
		Gson gson = Dep.get(Gson.class);
		Object exportCommand = gson.fromJson(json);
		System.out.println(exportCommand);
	}
	
}

package com.winterwell.moneyscript.lang;

import org.junit.Test;

import com.winterwell.moneyscript.lang.cells.CellSet;
import com.winterwell.moneyscript.lang.cells.Filter;
import com.winterwell.moneyscript.lang.cells.FilteredCellSet;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.utils.Printer;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.time.Time;

public class GroupRuleTest {

	@Test
	public void testParseConditionalGroup() {
		String s = "Pay from month 2: na";
		Lang lang = new Lang();
		Business b = lang.parse(s);
		GroupRule rule = (GroupRule) Containers.first(b.getAllRules());
		FilteredCellSet cells = (FilteredCellSet) rule.getSelector();
		Filter filter = cells.getFilter();
		System.out.println(filter);
	}


	@Test
	public void testConditionalGroup() {
		String s = "Staff:\n\tAlice: £1k\n\tBetty:£2k\n\nPay Rises from month 2:\n\tAlice: + £0.5k";
		Lang lang = new Lang();
		Business b = lang.parse(s);		
		
		b.getSettings().setStart(new Time(2020,1,1));
		b.getSettings().setEnd(new Time(2020,3,31));
		b.setSettings(b.getSettings()); // trigger columns
		
		b.run();
		
		Printer.out(b.toCSV());
		
		String stree = b.getRowTree().toString().trim();
		Printer.out(stree);
		assert stree.equals("Tree\n"
				+ "-Tree:Staff\n"
				+ "--Tree:Alice\n"
				+ "--Tree:Betty\n"
				+ "-Tree:Pay Rises");
		
		ArrayMap json = b.toJSON();
		double[] vs = b.getRow("Alice").getValues();
		assert vs[0] == 1000 && vs[1] == 1500;
//		Printer.out(json);
	}

	@Test
	public void testOverlappingGroups_firstOneWins() {
		String s = "Staff:\n\tAlice: £1k\n\tBetty:£2k\n\nPay Rises:\n\tAlice from Feb 2020: + £1k\n\tBetty from Feb 2020: + 2%\n";
		Lang lang = new Lang();
		Business b = lang.parse(s);		
		
		b.getSettings().setStart(new Time(2020,1,1));
		b.getSettings().setEnd(new Time(2020,3,31));
		b.setSettings(b.getSettings()); // trigger columns
		
		b.run();
		
		Printer.out(b.toCSV());
		
		String stree = b.getRowTree().toString().trim();
		Printer.out(stree);
		assert stree.equals("Tree\n"
				+ "-Tree:Staff\n"
				+ "--Tree:Alice\n"
				+ "--Tree:Betty\n"
				+ "-Tree:Pay Rises");
		
		ArrayMap json = b.toJSON();
//		Printer.out(json);
	}


	@Test
	public void testOverlappingGroups_groupRule() {
		String s = "Staff:\n\tAlice: £1k\n\tBetty:£2k\n\nPay Rises:\n\tStaff from Feb 2020: + £1k\n";
		Lang lang = new Lang();
		Business b = lang.parse(s);		
		
		b.getSettings().setStart(new Time(2020,1,1));
		b.getSettings().setEnd(new Time(2020,3,31));
		b.setSettings(b.getSettings()); // trigger columns
		
		b.run();
		
		Printer.out(b.toCSV());
		
		String stree = b.getRowTree().toString().trim();
		Printer.out(stree);
		assert stree.equals("Tree\n"
				+ "-Tree:Staff\n"
				+ "--Tree:Alice\n"
				+ "--Tree:Betty\n"
				+ "-Tree:Pay Rises");
		
		ArrayMap json = b.toJSON();
//		Printer.out(json);
	}
	

	@Test
	public void testNotOverlappingGroups() {
		String s = "Staff:\n\tAlice: £1k\n\tBetty:£2k\n\nNew Peeps:\n\tAlison from Feb 2020: £1k\n\tBetina from Feb 2020: £500\n";
		Lang lang = new Lang();
		Business b = lang.parse(s);		
		
		b.getSettings().setStart(new Time(2020,1,1));
		b.getSettings().setEnd(new Time(2020,3,1));
		b.run();
		
		Printer.out(b.toCSV());
		
		String stree = b.getRowTree().toString().trim();
		Printer.out(stree);
		assert stree.equals("Tree\n"
				+ "-Tree:Staff\n"
				+ "--Tree:Alice\n"
				+ "--Tree:Betty\n"
				+ "-Tree:New Peeps\n"
				+ "--Tree:Alison\n"
				+ "--Tree:Betina");
		
		ArrayMap json = b.toJSON();
//		Printer.out(json);
	}
	

}

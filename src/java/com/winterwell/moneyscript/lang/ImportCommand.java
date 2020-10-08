package com.winterwell.moneyscript.lang;

import java.io.File;
import java.io.StringReader;
import java.net.URI;
import java.util.List;

import com.winterwell.moneyscript.lang.num.Numerical;
import com.winterwell.moneyscript.output.Business;
import com.winterwell.moneyscript.output.Cell;
import com.winterwell.moneyscript.output.Col;
import com.winterwell.moneyscript.output.Row;
import com.winterwell.nlp.dict.Dictionary;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.io.CSVReader;
import com.winterwell.utils.io.CSVSpec;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;
import com.winterwell.utils.web.WebUtils;
import com.winterwell.web.FakeBrowser;

public class ImportCommand {

	public String src;
	protected boolean overwrite;

	@Override
	public String toString() {
		return "ImportCommand[src=" + src + "]";
	}

	public void run(Business b) {
		ImportCommand ic = this;
		String csv;
		// is it a file?
		if (ic.src.startsWith("file:")) {
			URI u = WebUtils.URI(ic.src);
			String fpath = u.getPath();
			csv = FileUtils.read(new File(fpath));
		} else { // fetch (TODO with some cache)
			FakeBrowser fb = new FakeBrowser();
			fb.setFollowRedirects(true);
			csv = fb.getPage(ic.src);
		}
		CSVSpec spec = new CSVSpec();
		CSVReader r = new CSVReader(new StringReader(csv), spec);
		// the first row MUST be headers
		r.setHeaders(r.next());
		r.setNumFields(-1); // flex
		Dictionary rowNames = b.getRowNames(); // do this now, so we can support fuzzy matching but not give a fuzzy
												// match against the csv's own rows
		List<String> headers = r.getHeaders();
		// match headers to columns
		String h1 = headers.get(1);
		int col1;
		// is it a time?
		try {
			Time time = TimeUtils.parseExperimental(h1);
			// HACK
			if (time.isBefore(b.getSettings().getStart())) {
				Dt dt = time.dt(b.getSettings().getStart());
				double i = dt.divide(b.getSettings().timeStep);
				col1 = (int) Math.round(1 - i); // i steps back from 1st col
			} else if (time.isAfter(b.getSettings().getStart())) {
				Log.w("Business.import", "Skip import " + h1);
				return; // import nothing
			} else {
				col1 = b.getColForTime(time).index;
			}
		} catch (Exception ex) {
			Log.w("Business.import", h1 + " > " + ex);
			col1 = 1;
		}

		for (String[] row : r) {
			if (row.length == 0)
				continue;
			String rowName = row[0];
			if (Utils.isBlank(rowName))
				continue;
			// match row name
			String ourRowName = run2_ourRowName(rowName, rowNames);
			if (rowName.toLowerCase().contains("balance")) {
				System.out.println(row);
			}
			Row brow = b.getRow(ourRowName);
			if (brow == null) {
				if (isEmptyRow(row)) {
					continue;
				}
				brow = new Row(ourRowName);
				b.addRow(brow);
			}
			for (int i = 1; i < row.length; i++) {
				String ri = row[i];
				double n = MathUtils.getNumber(ri);
				if (n == 0 && (ri==null || ! "0".equals(ri.trim()))) {
					continue; // skip blanks and non-numbers but not "true" 0s
				}
				int j = col1 + i - 1;
				if (j < 1) {
					// skip until we enter the sheet's time window
					continue;
				}
				Col col = new Col(j);
				Cell cell = new Cell(brow, col);
				Numerical v = new Numerical(n);
				v.comment = "import";
				// Set value
				b.state.set(cell, v);
			}
		}
	}

	/**
	 * 
	 * @param row
	 * @return false if any entries are non-zero
	 */
	private boolean isEmptyRow(String[] row) {
		// is it empty?
		for (int i = 1; i < row.length; i++) {
			String ri = row[i];
			double n = MathUtils.getNumber(ri);
			if (n != 0) {
				return false;
			}
		}
		return true;
	}

	private String run2_ourRowName(String rowName, Dictionary rowNames) {
		if (rowNames.contains(rowName)) {
			return rowNames.getMeaning(rowName);
		}
		String rn = StrUtils.toCanonical(rowName);
		if (rowNames.contains(rn)) {
			return rowNames.getMeaning(rn);
		}
		rn = rn.replaceAll("[^a-zA-Z0-9]", "");
		if (rowNames.contains(rn)) {
			return rowNames.getMeaning(rn);
		}
		// try removing "total" since MS group rows are totals
		if (rn.contains("total")) {
			String rn2 = rn.replace("total", "");
			if (!rn2.isBlank() && rowNames.contains(rn2)) {
				return rowNames.getMeaning(rn2);
			}
		}

		return StrUtils.toTitleCase(rowName);
	}

}

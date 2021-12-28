import React, { Component, useState } from 'react';
import { ReactDOM } from 'react-dom';
import _ from 'lodash';
import printer, { prettyNumber } from '../base/utils/printer';
import C from '../C';
import Roles from '../base/Roles';
import Misc from '../base/components/Misc';
import DataStore from '../base/plumbing/DataStore';
import Settings from '../base/Settings';
import ShareWidget, { ShareLink } from '../base/components/ShareWidget';
import ActionMan from '../plumbing/ActionMan';
import PropControl from '../base/components/PropControl';
import JSend from '../base/data/JSend';
import Tree from '../base/data/Tree';
import SimpleTable, { Column } from '../base/components/SimpleTable';
import { setTaskTags } from '../base/components/TaskList';
import ServerIO from '../plumbing/ServerIO';
import md5 from 'md5';
import { Alert, Badge, Nav, NavLink, NavItem, TabContent, TabPane } from 'reactstrap';
import LinkOut from '../base/components/LinkOut';
import { space, yessy } from '../base/utils/miscutils';
import PlanDoc from '../data/PlanDoc';
import { assert } from '../base/utils/assert';

/**
 * @param {?string[]} scenarios
 * @returns {PromiseValue<PlanResults>}
 */
const doShowMeTheMoney = ({ plandoc, scenarios }) => {
	if ( ! plandoc) return null;
	return DataStore.fetch(['transient', 'run', plandoc.id, md5(PlanDoc.text(plandoc) || 'blank'), str(scenarios) || 'base'],
		() => {
			let p = ServerIO.post('/money.json', { item: JSON.stringify(plandoc), scenarios });
			return p.then(JSend.data, res => {
				// error handling
				if (JSend.status(res) === 'fail') return JSend.data(res);
				throw res;
			});
		});
};


const fStyle = ({ cellValue, item, row, depth, column }) => {
	let cellStyle = {};
	let colVal = item && item[column.index];
	// no styling on blank/zero cells
	let css = cellValue && colVal && colVal.css;
	if (css) {
		let kvs = css.split(/[\n;]+/);
		kvs.forEach(kv => {
			// cleanup
			kv = kv.trim();
			if (!kv) return; // blank
			if (kv[kv.length - 1] === ";") kv = kv.substr(0, kv.length - 1); // should be redundant given the split() above
			// HACK to handle depth = weaker colour
			if (kv === ".bg-red") {
				cellStyle.background = "rgba(255,128,128, " + 1 / (1 + depth) + ")";
				return;
			}
			if (kv === ".bg-green") {
				cellStyle.background = "rgba(128,255,128, " + 1 / (1 + depth) + ")";
				return;
			}
			if (kv === ".bg-blue") {
				cellStyle.background = "rgba(128,128,255, " + 1 / (1 + depth) + ")";
				return;
			}
			let ki = kv.indexOf(':');
			let k = kv.substr(0, ki);
			let v = kv.substr(ki + 1);
			if (k && v) cellStyle[k] = v;
		});
	}
	// red -ives
	if (cellValue && cellValue < 0) cellStyle.color = "red";
	// actuals
	if (colVal && colVal.comment && (colVal.comment === "import" || colVal.comment.substr(0, 7) === "imports")) {
		cellStyle.color = "blue"; // cellStyle.backgroundColor = "#ddf"; // blue pastel highlight on actuals
	}
	// column hacks
	if (column.Header) {
		if (column.Header.toLowerCase().includes("total")) {
			cellStyle.fontWeight = "bold";
			cellStyle.borderRight = "2px solid black"; // end of year marker line
		}
		if (column.Header.toLowerCase().includes('dec')) cellStyle.borderRight = "2px solid black"; // end of year marker line
		if (column.Header.toLowerCase() === 'row') cellStyle.borderRight = "2px solid black"; // start numbers marker line
	}
	return cellStyle;
};


const renderCell = (v, column, item) => {
	const colv = item[column.index];
	let vs = (colv && colv.str) || v || '';
	vs = vs.replace('-', '‑'); // str value for display, then replace - with a non-breaking hyphen (which looks the same here, but it is different)	
	if (colv && colv.delta) {
		return <div>{vs} <span className='small text-info'>delta: {prettyNumber(colv.delta, 3)}</span></div>;
	}
	return vs;
};


const ViewSpreadSheet = ({ plandoc, scenarios, hideMonths }) => {
	if (!plandoc) return null;
	const pvrun = doShowMeTheMoney({ plandoc, scenarios });
	if (!pvrun.resolved) {
		return <Misc.Loading />;
	}
	const runOutput = pvrun.value;
	// eror?
	if (!runOutput || yessy(runOutput.errors)) {
		const errors = (runOutput && runOutput.errors) || [pvrun.error];
		return errors.map(e => <Alert color="danger" key={JSON.stringify(e)}>{JSON.stringify(e)}</Alert>);
	}
	let tabId = 1*(DataStore.getUrlValue("tab") || 0);
	const sheetId = plandoc.sheets[tabId].id;
	assert(sheetId, "ViewSpreadsheet.jsx - no sheetId for tabId "+tabId);
	// only process the data once (so the Tree is stable)
	if ( ! runOutput.dataTree || ! runOutput.dataTree[sheetId]) {
		makeDataTree({ runOutput, sheetId });
	} // data prep done

	// HACK - only show year totals
	let vizcolumns = runOutput.allcolumns;
	if (hideMonths) {
		vizcolumns = vizcolumns.filter(col => col.Header === "Row" || col.Header.includes("Total"));
	}

	const dtree = runOutput.dataTree[sheetId];
	console.log("dataTree", dtree, "allcolumns", runOutput.allcolumns);
	// The Table
	
	return (<>
	<Nav tabs>
			{plandoc.sheets.map((sheet, i) => (<NavItem key={i} className={tabId===i? 'active' : "text-secondary"}>
				<NavLink 
					onClick={() => DataStore.setUrlValue("tab", i)}
					className={space(tabId===i && 'active text-primary font-weight-bold')}
				>{sheet.title || "Sheet "+(i+1)}
				</NavLink>
				</NavItem>)
			)}
		</Nav>
		<TabContent activeTab={tabId}>
			<TabPane tabId={tabId}>
	<SimpleTable
		tableName={plandoc.name}
		dataTree={dtree}
		columns={vizcolumns}
		showSortButtons={false}
		scroller
		hasCollapse
		hideEmpty={false}
		hasCsv />
		</TabPane></TabContent></>);
};

const InfoPop = ({ text }) => {
	if (!text) return null;
	return <Badge className='ml-1 mr-1' color='info' pill title={text}>i</Badge>;
};

/**
 * 
 * @param {*} runOutput Modified here 
 */
const makeDataTree = ({ runOutput, sheetId}) => {
	assert(runOutput);
	assert(sheetId, "no sheet ID");
	// let rows = runOutput.rows;
	let rowtree = runOutput.parse.rowtree;
	let dataForRow = runOutput.dataForRow;
	let thisSheetRowNames = runOutput.rowsForPlansheet && runOutput.rowsForPlansheet[sheetId];
	// Make the columns
	// type: Column[]
	let columns = runOutput.columns.map((c, i) => {
		// red -ives (hack string test)
		let column = new Column({
			index: i,
			accessor: r => r[i] && (r[i].v || r[i].str), // get the numerical value for csv export
			Cell: renderCell,
			tooltip: ({ cellValue, item, column }) => item && item[i] && item[i].comment,
			Header: c,
			style: fStyle
		});
		return column;
	});
	// The row name TH column
	let rowCol = new Column({
		accessor: 'row', Header: 'Row',
		Cell: (v, column, item) => {
			let row = runOutput.rows.find(r => r.name === v);
			let comment = row && row.comment;
			return <>{v}{comment && <InfoPop text={comment} />}</>;
		},
		tooltip: ({ cellValue, item, column }) => {
			let row = runOutput.rows.find(r => r.name === cellValue);
			return row && row.comment;
		},
		style: fStyle
	});
	runOutput.allcolumns = [rowCol].concat(columns);

	// which rows?

	// transform rowtree from holding names, to holding all the values
	if ( ! runOutput.dataTree) runOutput.dataTree = {};
	const dtree = Tree.map(rowtree, node => {
		let rowName = node.value;
		if ( ! rowName) {
			// root!
			return new Tree();
		}
		if (thisSheetRowNames && ! thisSheetRowNames.includes(rowName)) {
			return null;
		}
		// NB: rowData is an array but js will let us treat it like an object
		let rowData = dataForRow[rowName];
		rowData.row = rowName;
		rowData.id = rowName; // needs and id for collapse to work
		// rowData.style = {};
		return rowData;
	});
	assert(dtree);
	runOutput.dataTree[sheetId] = dtree;
	console.log("MADE dataTree", runOutput.dataTree, "allcolumns", runOutput.allcolumns);
};

export { doShowMeTheMoney };
export default ViewSpreadSheet;

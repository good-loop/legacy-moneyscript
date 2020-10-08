import React, { Component, useState } from 'react';
import {ReactDOM} from 'react-dom';
import _ from 'lodash';
import {assert} from 'sjtest';
import {Login} from 'you-again';
import printer from '../base/utils/printer';
import C from '../C';
import Roles from '../base/Roles';
import Misc from '../base/components/Misc';
import DataStore from '../base/plumbing/DataStore';
import Settings from '../base/Settings';
import ShareWidget, {ShareLink} from '../base/components/ShareWidget';
import ActionMan from '../plumbing/ActionMan';
import PropControl from '../base/components/PropControl';
import JSend from '../base/data/JSend';
import Tree from '../base/data/Tree';
import SimpleTable, {Column} from '../base/components/SimpleTable';
import {setTaskTags} from '../base/components/TaskList';
import ServerIO from '../plumbing/ServerIO';
import md5 from 'md5';
import { Alert } from 'reactstrap';

/**
 * @returns {PromiseValue<PlanResults>}
 */
const doShowMeTheMoney = ({plandoc}) => {
	if ( ! plandoc) return null;
	return DataStore.fetch(['transient', 'run', plandoc.id, md5(plandoc.text || '')],
		() => {
			let p = ServerIO.post('/money.json', {text: plandoc.text});
			return p.then(JSend.data, res => {
				// error handling
				if (JSend.status(res) === 'fail') return JSend.data(res);
				throw res;
			});
		});
};


const fStyle = ({cellValue, item, row, depth, column}) => {
	let cellStyle = {};
	let colVal = item && item[column.index];
	// no styling on blank/zero cells
	let css = cellValue && colVal && colVal.css;
	if (css) {
		let kvs = css.split("[\n;]");
		kvs.forEach(kv => {
			// HACK
			if (kv === ".bg-red") {
				cellStyle.background = "rgba(255,128,128, "+1/(1+depth)+")";
				return;
			}
			if (kv === ".bg-green") {
				cellStyle.background = "rgba(128,255,128, "+1/(1+depth)+")";
				return;
			}
			if (kv === ".bg-blue") {
				cellStyle.background = "rgba(128,128,255, "+1/(1+depth)+")";
				return;
			}
			let ki = kv.indexOf(':');
			let k = kv.substr(0, ki);
			let v = kv.substr(ki+1);
			if (k && v) cellStyle[k] = v;
		});
	}
	// red -ives
	if (cellValue && cellValue < 0) cellStyle.color = "red";
	// actuals
	if (colVal && colVal.comment==="import") cellStyle.color = "blue"; // cellStyle.backgroundColor = "#ddf"; // blue pastel highlight on actuals
	// column hacks
	if (column.Header) {
		if (column.Header.toLowerCase()==='total') cellStyle.fontWeight = "bold";
		if (column.Header.toLowerCase().includes('dec')) cellStyle.borderRight = "2px solid black"; // end of year marker line
		if (column.Header.toLowerCase() === 'row') cellStyle.borderRight = "2px solid black"; // start numbers marker line
	}
	return cellStyle;
};



const ViewSpreadSheet = ({plandoc}) => {
	if ( ! plandoc) return null;
	const pvrun = doShowMeTheMoney({plandoc});
	if ( ! pvrun.resolved) {
		return <Misc.Loading />;
	}
	const runOutput = pvrun.value;
	if (runOutput.errors) {
		return <Alert>{runOutput.errors.map(e => <div key={JSON.stringify(e)}>{JSON.stringify(e)}</div>)}</Alert>;
	}
	// only process the data once (so the Tree is stable)
	if ( ! runOutput.dataTree) {
		// let rows = runOutput.rows;
		let rowtree = runOutput.parse.rowtree;
		let dataForRow = runOutput.dataForRow;
		// Make the columns
		// type: Column[]
		let columns = runOutput.columns.map((c,i) => {
			// red -ives (hack string test)
			return new Column({
				index: i,
				accessor: r => r[i] && (r[i].v || r[i].str), // get the numerical value for csv export
				Cell: (v, column, item) => ((item[column.index] && item[column.index].str) || v || '').replace('-', '‑'), // str value for display, then replace - with a non-breaking hyphen
				tooltip: ({cellValue, item, column}) => item && item[i] && item[i].comment,
				Header: c,
				style: fStyle
			});
		});	
		let rowCol = new Column({
			accessor:'row', Header:'Row',
			// Cell: (v, column, item) => ((item[column.index] && item[column.index].str) || v || '').replace('-', '‑'), // str value for display, then replace - with a non-breaking hyphen
			// tooltip: ({cellValue, item, column}) => item && item[i] && item[i].comment,			
			style: fStyle
		});
		runOutput.allcolumns = [rowCol].concat(columns);		

		// transform rowtree from holding names, to holding all the values
		runOutput.dataTree = Tree.map(rowtree, node => {
			let rowName = node.value;
			if ( ! rowName) {
				// root!
				return new Tree();
			}
			// NB: rowData is an array but js will let us treat it like an object
			let rowData = dataForRow[rowName];
			rowData.row = rowName;
			rowData.id = rowName; // needs and id for collapse to work
			// rowData.style = {};
			return rowData;
		});
		console.log("MADE dataTree", runOutput.dataTree, "allcolumns", runOutput.allcolumns);
	} // data prep done

	console.log("dataTree", runOutput.dataTree, "allcolumns", runOutput.allcolumns);
	// The Table
	return <>
		<SimpleTable 
			tableName={plandoc.name}
			dataTree={runOutput.dataTree}
			columns={runOutput.allcolumns} 			 
			showSortButtons={false} 
			scroller 
			hasCollapse 
			hideEmpty={false}
			csv /></>;
};

export {doShowMeTheMoney};
export default ViewSpreadSheet;

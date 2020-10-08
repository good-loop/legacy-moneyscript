
import React, { Component } from 'react';
import {ReactDOM} from 'react-dom';
import {SJTest} from 'sjtest';
import {Login} from 'you-again';
import printer from '../base/utils/printer';
import C from '../C';
import Roles from '../base/Roles';
import Misc from '../base/components/Misc';
import {stopEvent} from '../base/utils/miscutils';
import DataStore from '../base/plumbing/DataStore';
import Settings from '../base/Settings';
import ShareWidget, {ShareLink} from '../base/components/ShareWidget';
import ListLoad, {CreateButton, ListItems} from '../base/components/ListLoad';
import ActionMan from '../plumbing/ActionMan';
import PropControl from '../base/components/PropControl';
import JSend from '../base/data/JSend';
import ServerIO from '../plumbing/ServerIO';
// import ChartWidget from '../base/components/ChartWidget';
import {doShowMeTheMoney} from './ViewSpreadsheet';
import md5 from 'md5';

const ViewCharts = ({plandoc, path}) => {
	if (true) return <div>TODO</div>;
	// if ( ! plandoc) return null;
	// const pvrun = doShowMeTheMoney({plandoc});
	// const runOutput = pvrun.value;
	// if ( ! runOutput) return null;	
	
	// let dataFromLabel = {};
	// // let rowOn = DataStore.getValue('widget','ViewCharts','rowOn') || {};
	// let rows = runOutput.rows; // Object.keys(rowOn).filter(r => rowOn[r]);
	// let columns = runOutput.columns;
	// let times = columns;
	// rows.forEach(r => {
	// 	let row = runOutput[r]; // [{v:Number}]
	// 	let valForTime = {};
	// 	row.forEach(({v}, i) => {
	// 		valForTime[times[i]] = v;
	// 	});
	// 	dataFromLabel[r] = valForTime;// {'Jan 2019': 10};
	// });
	// // console.warn(runOutput);

	// return (<div>
	//  	<ChartWidget off dataFromLabel={dataFromLabel} />
	// </div>);
};

export default ViewCharts;

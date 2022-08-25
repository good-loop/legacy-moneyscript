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
import {Chart, ctx} from 'chart.js'
import { setTaskTags } from '../base/components/TaskList';
import ServerIO from '../plumbing/ServerIO';
import md5 from 'md5';
import { Alert, Badge, Nav, NavLink, NavItem, TabContent, TabPane } from 'reactstrap';
import LinkOut from '../base/components/LinkOut';
import { ellipsize, space, yessy } from '../base/utils/miscutils';
import PlanDoc from '../data/PlanDoc';
import { assert } from '../base/utils/assert';
import NewChartWidget from '../base/components/NewChartWidget'


/**
data={runOutput}
		tableName={plandoc.name}
		dataTree={dtree}
		columns={vizcolumns} 
 */

const Graphs = ({data}) => {
	// The Table	

	console.log("here!", data.parse.charts)

	var charts = data.parse.charts.map((selection) => {
		let myLabels = data.columns
		let myDatasets = []
		var rowSelections = (Object.values(selection.selector))
		console.log(data.parse)

		if(rowSelections.length > 1){
			rowSelections = rowSelections.map((el) => el.rowName)
		}

		rowSelections.forEach((rowName) => {
			myDatasets.push({
				label: rowName,
				data: data.dataForRow[rowName].map((el) => el.v)})
		})
		console.log(rowSelections)
		return (<>
			<div className="chart-container" style={{height:"500px"}}>
				<NewChartWidget
				responsive = "true"
				maintainAspectRatio = "false"
				data={
					{labels: myLabels,
					datasets: myDatasets}}
				className="test"
				style={{height:"500px", width:"750px"}}
				/>
			</div>
			<br />
			</>); 
	})

	return (<>{charts}</>)
	
};

export default Graphs;

import React, { Component, useState } from 'react';
import {ReactDOM} from 'react-dom';
import printer from '../base/utils/printer';
import { Alert, Col, Form, Row } from 'reactstrap';
import C from '../C';
import Roles from '../base/Roles';
import Misc from '../base/components/Misc';
import {stopEvent, modifyHash, encURI} from '../base/utils/miscutils';
import DataStore from '../base/plumbing/DataStore';
import Settings from '../base/Settings';
import ShareWidget, {ShareLink} from '../base/components/ShareWidget';
import ListLoad, {CreateButton} from '../base/components/ListLoad';
import ActionMan from '../plumbing/ActionMan';
import PropControl from '../base/components/PropControl';
import JSend from '../base/data/JSend';
import SimpleTable from '../base/components/SimpleTable';
import {setTaskTags} from '../base/components/TaskList';
import ServerIO from '../plumbing/ServerIO';
import ViewCharts from './ViewCharts';
import ViewSpreadSheet, {doShowMeTheMoney} from './ViewSpreadsheet';
import ChartWidget from '../base/components/ChartWidget';
import _ from 'lodash';
import { getPlanId } from './MoneyScriptEditorPage';


const ChartSettings = ({id, rowNames}) => {
	
	// turn row names into object of "{RowName1: false, RowName2: False, ...}"
	let [rowSelections, setRowSelections] = useState(rowNames.reduce((acc, val) => {return {...acc, [val]: false}}, {}))

	console.log(rowNames)
	let checkboxes = (
		<div className="checkboxes">
			{rowNames.map((el) => {return (<p>{el}</p>)})}
		</div>
	)

	console.log(checkboxes)

	return (
		<div 
			className="chart-settings"
			style={{backgroundColor:"#EEE", border:"1px solid Gray", borderRadius:"10px", padding:"10px", height:"100%"}}
		>
			{checkboxes}
			<p>{id}</p>
		</div>
	)
}

const ChartChunk = ({id}) => {
	return (
		<div className="chart-chunk" style={{height:"100%", backgroundColor:"#EEE", border:"1px solid Gray", borderRadius:"10px", padding:"10px"}}
		>
			<p>{id}</p>
		</div>
	)
}


const ChartPage = () => {

	// a chart set should contain its settings and its chart object
	let [chartSets, setChartSets] = useState([])

	const id = getPlanId();
	if ( ! id) {
		return <BS.Alert color='warning'>No plan ID - go to <a href='#Plan'>Plans</a> to select or create one</BS.Alert>;
	}
	// load
	const type = C.TYPES.PlanDoc;
	const pvItem = ActionMan.getDataItem({type, id, status:C.KStatus.DRAFT});
	if ( ! pvItem.value) {
		return (<div><h1>{type}: {id}</h1><Misc.Loading /></div>);
	}
	const plandoc = pvItem.value;

	const pvrun = doShowMeTheMoney({plandoc});
	if ( ! pvrun.resolved) {
		return <Misc.Loading />;
	}
	const runOutput = pvrun.value;
	if (runOutput.errors) {
		return <Alert>{runOutput.errors.map(e => <div key={JSON.stringify(e)}>{JSON.stringify(e)}</div>)}</Alert>;
	}
	let rows = runOutput.rows || [];


	console.log("rows: ",pvrun)

	let chartExample = (<><br />
	<div className="chart-set" style={{height:"70vh"}}>
		<Row style={{minHeight:"100%"}}>
			<Col md={4}><ChartSettings id={id} rowNames={rows.map((el) => el.name)}/></Col>
			<Col md={1}></Col>
			<Col md={7}><ChartChunk id={id}/></Col>
		</Row>
	</div><br /></>)

	if (true) {
		return (
			<div id="chart-page">
				<div className='header'>
					<Row className="w-100">
					<Col md={2}><a className='mt-1 btn btn-dark' 				
						href={'/#sheet/'+encURI(id)+"?tab="+(DataStore.getUrlValue("tab")||"")}>&lt; View Sheet</a></Col>
					<Col md={8}><h2>{"CHART PAGE TESTING PAGE"}</h2></Col>
					</Row>	
				</div>
				<div className="charts-body">
					{chartExample}
					{chartExample}
					{chartExample}
				</div>
			</div>
		)
	}
		
};

export default ChartPage;

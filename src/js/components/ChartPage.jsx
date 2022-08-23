import React, { Component, useState } from 'react';
import {ReactDOM} from 'react-dom';
import printer from '../base/utils/printer';
import { Alert, Button, Col, Form, Row } from 'reactstrap';
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
import ViewSpreadSheet, {doShowMeTheMoney, makeDataTree} from './ViewSpreadsheet';
import ChartWidget from '../base/components/ChartWidget';
import _, { cloneDeep } from 'lodash';
import { getPlanId } from './MoneyScriptEditorPage';
import NewChartWidget from '../base/components/NewChartWidget'



const ChartSettings = ({id, rowNames, selections, setSelections}) => {
	console.log(rowNames)
	let [searchValue, setSearchValue] = useState("")


	const selected = (
		<div className="checkboxes">
			{selections.map((el) => {return (
			<Row>
				<Col md={9}><p>{el}</p></Col>
				<Col md={3}><Button className='mt-1 btn btn-dark' onClick={() => removeSelection(el)}>-</Button></Col>
			</Row>
			)})}
		</div>
	)
	
	const removeSelection = (toRemove) => setSelections(selections.filter((el) => el !== toRemove))

	const addSelection = (newSelection) => {
		if(rowNames.includes(newSelection)){
			setSelections([...selections, newSelection]);
			setSearchValue("")
		}
	}
	return (
		<div 
		className="chart-settings"
		style={{backgroundColor:"#EEE", border:"1px solid Gray", borderRadius:"10px", padding:"10px", height:"100%"}}>
			<textarea type="text" value={searchValue} onChange={e => setSearchValue(e.target.value)}></textarea>
			<Button className='mt-1 btn btn-dark' onClick={() => {addSelection(searchValue)}}>Add</Button>
			{selected}
			<p>{id}</p>
		</div>
	)
}

const ChartVisuals = ({id, selections, data}) => {
	let myLabels = data.columns.filter((el) => (el.indexOf("Total") == -1))
	console.log(myLabels)
	let myDatasets = []
	selections.forEach((row) => {
		myDatasets.push({
			label:row,
			data: data.dataForRow[row].filter((el) => el.comment.indexOf("total for year") == -1).map((el) => el.v)})
		})
	

	return (
		<div className="chart-chunk" style={{height:"100%", backgroundColor:"#EEE", border:"1px solid Gray", borderRadius:"10px", padding:"10px"}}>
			<NewChartWidget
				type="bar"
				responsive = "true"
				maintainAspectRatio = "false"
				data = {
					{labels: myLabels,
					datasets: myDatasets}}
				className="test"
				style={{height:"100%", width:"100%"}}
			/>
		</div>
	)
}

const ChartChunk = ({id, rows, data}) => {
	let [selections, setSelections] = useState(["Salaries"])

	let chartExample = (<><br />
	<div className="chart-set" style={{height:"70vh"}}>
		<Row style={{minHeight:"100%"}}>
			<Col md={4}><ChartSettings id={id} rowNames={rows.map((el) => el.name)} selections={selections} setSelections={setSelections}/></Col>
			<Col md={1}></Col>
			<Col md={7}><ChartVisuals id={id} selections={selections} data={data}/></Col>
		</Row>
	</div><br /></>)

	return chartExample
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

	console.log(pvItem, "OOOOOOOO")

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

	let dataForest = plandoc.sheets.map((sheet) =>{
		let outputClone = cloneDeep(runOutput)
		let sheetId = sheet.id
		makeDataTree({ runOutput:outputClone, sheetId });
		console.log(sheetId, outputClone)
		return outputClone.dataTree[sheetId]
	})

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
					<ChartChunk id={id} rows={rows} data={runOutput}/>
				</div>
			</div>
		)
	}
		
};

export default ChartPage;

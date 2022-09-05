import React, { Component, useState } from 'react';
import { ReactDOM } from 'react-dom';
import printer from '../base/utils/printer';
import { Alert, Button, Container, Col, Dropdown, DropdownItem, DropdownMenu, DropdownToggle, Form, Row, Popover, PopoverHeader, PopoverBody } from 'reactstrap';
import C from '../C';
import CSS from '../base/components/CSS';
import Roles from '../base/Roles';
import Misc from '../base/components/Misc';
import { stopEvent, modifyHash, encURI, asArray } from '../base/utils/miscutils';
import DataStore, { getDataPath } from '../base/plumbing/DataStore';
import Settings from '../base/Settings';
import ShareWidget, { ShareLink } from '../base/components/ShareWidget';
import ListLoad, { CreateButton } from '../base/components/ListLoad';
import ActionMan from '../plumbing/ActionMan';
import PropControl from '../base/components/PropControl';
import JSend from '../base/data/JSend';
import SimpleTable from '../base/components/SimpleTable';
import { setTaskTags } from '../base/components/TaskList';
import ServerIO from '../plumbing/ServerIO';
import ViewCharts from './ViewCharts';
import ViewSpreadSheet, { doShowMeTheMoney, makeDataTree } from './ViewSpreadsheet';
import ChartWidget from '../base/components/ChartWidget';
import _, { cloneDeep, remove } from 'lodash';
import { getPlanId } from './MoneyScriptEditorPage';
import NewChartWidget from '../base/components/NewChartWidget'
import deepCopy from '../base/utils/deepCopy';
import KStatus from '../base/data/KStatus';
import SavePublishDeleteEtc from '../base/components/SavePublishDeleteEtc';
import { assert } from '../base/utils/assert';

class ChartLine {
	rowName;
	color;	
};
class ChartSetup {
	/** @type {string} */
	type;
	/** @type {ChartLine[]} */
	lines = [];
}

/** component for a charts setting & visuals  */
const ChartChunk = ({id, plandoc, rows, scenarios }) => {
	if ( ! plandoc) return null;
	// recalc for scenario specifics
	const pvrun = doShowMeTheMoney({ plandoc, scenarios });
	if ( ! pvrun.resolved) {
		return <Misc.Loading />;
	}

	let data = pvrun.value
	// HACK only one chart
	let chartPath = getDataPath({status:KStatus.DRAFT, id, type:C.TYPES.PlanDoc}).concat("charts", 0);
	let chartSetup = DataStore.setValueIfAbsent(chartPath, new ChartSetup());

	return (
		<Row className="chart-set">
				<Col md={4}>
					<ChartSettings
						plandoc={plandoc}
						chartPath={chartPath}
						rowNames={rows.map((el) => el.name)}
					/>
				</Col>
				<Col md={8}><ChartVisuals chartSetup={chartSetup} data={data} /></Col>
			</Row>
	);
}

const ChartSettings = ({plandoc, chartPath, rowNames}) => {
	let chartSetup = DataStore.getValue(chartPath);
	assert(chartSetup); // guaranteed from ChartChunk
	return <>		
		<PropControl label="Type" prop="type" type="select" options={["line","bar"]} path={chartPath} warnOnUnpublished={false} />
		<PropControl label="Chart Lines" itemType="Line" type="list" prop="lines" path={chartPath} Viewer={false} rowStyle
			Editor={args => <ChartLineEditor rowNames={rowNames} {...args} />} />
	</>;
};

const ChartLineEditor = ({item, rowNames, path}) => {
	return <div className='row'>
		<PropControl className="col" label="Row" prop="rowName" type="select" options={rowNames} path={path} warnOnUnpublished={false} />
		<PropControl className="col" label="Colour" prop="color" type="color" path={path} warnOnUnpublished={false} />
	</div>;
};


const ScenariosOnOff = ({scenarioMap, scenarioTexts}) => {
	if ( ! scenarioMap) return null;
	return (<Form>
			<PropControl inline label="Scenarios" tooltip={"Toggle scenarios on/off"} 
				type='checkboxArray' prop='scenarios' options={Object.keys(scenarioMap)} 
				tooltips={scenarioTexts}
			/>
		</Form>);
};

/**
 * 
 * @param {Object} p
 * @param {ChartSetup} p.chartSetup
 * @param {Object} p.data
 * @returns 
 */
const ChartVisuals = ({chartSetup, data }) => {
	let type = chartSetup.type;
	// columns (skip annual totals)
	let colMask = data.columns.map(colName => ! colName.includes("Total"))
	let myLabels = data.columns.filter((el, i) => colMask[i]);
	// HACK in case it is not an array
	let lines = asArray(chartSetup.lines);
	let myDatasets = [];
	for(let i=0; i<lines.length; i++) {
		let chartLine = lines[i];
		let lineData = data.dataForRow[chartLine.rowName]?.filter((_cell,i) => colMask[i]).map(cell => cell.v)
		if ( ! lineData) { // paranoia
			console.warn("No data for "+chartLine.rowName, chartLine, data.dataForRow[chartLine.rowName]);
			continue;
		}		
		// add to the datasets
		myDatasets.push({
			label: chartLine.rowName,
			data: lineData,
			borderColor: chartLine.color,
			backgroundColor: chartLine.color
		});
	}

	return (
		<div className="chart-visual-container">
			<NewChartWidget
				type={type}
				responsive="true"
				maintainAspectRatio="false"
				data={
					{
						labels: myLabels,
						datasets: myDatasets
					}}
				className="chart-visual"
				style={{ height: "100%", width: "100%" }}
			/>
		</div>
	)
};

const ChartPage = () => {
	const id = getPlanId();
	if (!id) {
		return <BS.Alert color='warning'>No plan ID - go to <a href='#Plan'>Plans</a> to select or create one</BS.Alert>;
	}
	// load
	const type = C.TYPES.PlanDoc;
	const pvItem = ActionMan.getDataItem({ type, id, status: C.KStatus.DRAFT });
	if (!pvItem.value) {
		return (<div><h1>{type}: {id}</h1><Misc.Loading /></div>);
	}

	let _scenarios = DataStore.getUrlValue("scenarios");
	console.log(_scenarios, "scenarios")

	let scenariosOn = _.isString(_scenarios)? _scenarios.split(",") : _scenarios; // NB: string if fresh from url, array if modified by PropControl

	const plandoc = pvItem.value;


	const pvrun = doShowMeTheMoney({ plandoc });
	if (!pvrun.resolved) {
		return <Misc.Loading />;
	}
	const runOutput = pvrun.value;
	if (runOutput.errors) {
		return <Alert>{runOutput.errors.map(e => <div key={JSON.stringify(e)}>{JSON.stringify(e)}</div>)}</Alert>;
	}
	let rows = runOutput.rows || [];

	let scenarioMap = pvrun.value && pvrun.value.scenarios;

	
	return (
		<>
			<CSS css={`footer {display:none}`} />
			<div className='header'>
				<Row className="w-100">
					<Col md={2}><a className='mt-1 btn btn-dark'
						href={'/#sheet/' + encURI(id) + "?tab=" + (DataStore.getUrlValue("tab") || "")}>&lt; View Sheet</a></Col>
					<Col md={8}><h2>{"CHARTS PAGE"}</h2></Col>
				</Row>
				<Row>
					<ScenariosOnOff scenarioMap={scenarioMap} scenarioTexts={pvrun.value && pvrun.value.scenarioTexts} />
				</Row>
			</div>
			<div className="clearfix"></div>
			<div className="chart">
				<ChartChunk plandoc={plandoc} id={id} rows={rows} data={runOutput} scenarios={scenariosOn} />
			</div>
			<SavePublishDeleteEtc autoSave hidden id={id} type={C.TYPES.PlanDoc} cannotPublish cannotDelete />
		</>
	);
};

export default ChartPage;

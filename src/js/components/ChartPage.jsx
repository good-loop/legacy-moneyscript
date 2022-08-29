import React, { Component, useState } from 'react';
import { ReactDOM } from 'react-dom';
import printer from '../base/utils/printer';
import { Alert, Button, Container, Col, Dropdown, DropdownItem, DropdownMenu, DropdownToggle, Form, Row, Popover, PopoverHeader, PopoverBody } from 'reactstrap';
import C from '../C';
import CSS from '../base/components/CSS';
import Roles from '../base/Roles';
import Misc from '../base/components/Misc';
import { stopEvent, modifyHash, encURI } from '../base/utils/miscutils';
import DataStore from '../base/plumbing/DataStore';
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


// Each selected row that we want in our chart
// In the end, result is: 'RowName   [Colour Button]   [Remove Button]'
const ChartSelectionRow = ({selections, setSelections, el}) => {
	let [colorPopoverOpen, setColorPopoverState] = useState(false)
	let [colorPropOpen, setColorPropOpen] = useState(false)

	// find & remove row we've selected by name
	const removeSelection = (toRemove) => {
		setSelections(selections.filter((el) => el[0] !== toRemove[0]))
	}

	// remaps the selected rows colour
	const changeColor = ((el, color) => {
		setSelections(selections.map((sel) => {
			if(sel[0] == el) { return [sel[0], color] }
			return sel
		}))
	})

	// When choosing colours, have it pop out into a wee "choose your own" window
	const ColorPopover = (el) => {
		let popoverId = el[0].split(" ").join("-")

		const onMouseLeave = (e) => {
			if(colorPropOpen){
				setColorPopoverState(!colorPopoverOpen);
				setColorPropOpen(false)
			}
		}
		

		return (
			<>
				<Button id={popoverId} className='colorPopover' onClick={() => { setColorPopoverState(!colorPopoverOpen) }} style={{ backgroundColor:el[1]}}>
					Color
				</Button>
				<Popover placement='bottom' isOpen={colorPopoverOpen} target={popoverId} toggle={() => { setColorPopoverState(!colorPopoverOpen) }}>
					<PopoverHeader>{el[0]} color</PopoverHeader>
					<PopoverBody className='Popover-body'>
						<PropControl onMouseLeave={onMouseLeave} type="color" prop="mycol" onChange={(e) => {changeColor(el[0], e.value)}} onClick={() => setColorPropOpen(true)}/>
					</PopoverBody>
				</Popover>
			</>
		)
		}

	return (<>
	<Row className="selection-row">
		<Col lg={4} md={4} className="row-text"><p>{el[0]}</p></Col>	
		<Col lg={1} md={2}></Col>
		<Col lg={3} md={2}l>{ColorPopover(el)}</Col>									
		<Col lg={1} md={2}></Col>
		<Col lg={3} md={2}><Button className='btn btn-dark' onClick={() => removeSelection(el)}>-</Button></Col>
	</Row>
	<br />
	</>)
}

// which rows are selected to be displayed
const ChartSelections = ({ rowNames, selections, setSelections }) => {
	let [searchValue, setSearchValue] = useState("")

	// choose row by name, check it matches a row in the spreadsheet & hasn't been chosen, then add it in
	const addSelection = (newSelection) => {
		let newSelectionLower = deepCopy(newSelection).toLowerCase()
		let rowNamesLower = rowNames.map((row) => deepCopy(row).toLowerCase())
		
		// if it's not already chosen...
		if(selections.map((el) => el[0].toLowerCase()).includes(newSelectionLower) == false) {
			// try to find that row, (converting uppercase to lowercase) 
			let selectedRow = rowNames.filter((row) => row.toLowerCase() === newSelectionLower)[0]
			// if found, add it to our selections
			if(selectedRow) {
				setSelections([...selections, [selectedRow, "#AAA"]]);
				setSearchValue("")
			}
		}
	}

	// holder of all the rows we've currently got selected
	const selected = (
		<div className="selection-chunk">
			{selections.map((el) => {
				return (<ChartSelectionRow el={el} selections={selections} setSelections={setSelections}/>)
			})}
		</div>
	)

	// TODO: a textarea search sucks! It currently needs to be an exact match or it doesn't 
	return (
		<div className="chart-selection-settings">
			<div className="selection-input">
				<Row>
					<Col md={9}><textarea style={{marginTop:"5px"}} className='selection-search' rows="1" type="text" value={searchValue} onChange={e => setSearchValue(e.target.value)}></textarea></Col>
					<Col md={3}><Button className='btn btn-dark add-selection' onClick={() => { addSelection(searchValue) }}>+</Button></Col>
				</Row>
				<br />
				{selected}
			</div>
		</div>
	)
}


// settings for ChartVisual
//TODO: - time range settings?
// 		- adding in the scenarios?
//		- more styling, eg filled in line graphs
//		- export graph to image?
const ChartSettings = ({ id, rowNames, selections, setSelections, setChartType, typeDropdownState, setTypeDropdownState }) => {
	return (
		<div className="chart-settings">
			<ChartSelections rowNames={rowNames} selections={selections} setSelections={setSelections} />
			<hr className='line'/>
			<ChartTypeDropdown setChartType={setChartType} typeDropdownState={typeDropdownState} setTypeDropdownState={setTypeDropdownState} />
			<br />

		</div>
	)
}
// component for a charts setting & visuals
const ChartChunk = ({ plandoc, id, rows, scenarios }) => {
	let [selections, setSelections] = useState([])	// TODO: refactor this into an object, as an array it's needlessly complicated
	let [typeDropdownState, setTypeDropdownState] = useState(false)
	let [chartType, setChartType] = useState("line") // TODO: add more types, only tested with 'line' & 'bar' but others *should* work fine


	// recalc for scenario specifics
	if (!plandoc) return null;
	const pvrun = doShowMeTheMoney({ plandoc, scenarios });
	if (!pvrun.resolved) {
		return <Misc.Loading />;
	}

	let data = pvrun.value

	let chartExample = (<><br />
		<div className="chart-set">
			<Row className=""style={{ maxHeight: "100%" }}>
				<Col md={4}>
					<ChartSettings
						id={id}
						rowNames={rows.map((el) => el.name)}
						selections={selections}
						setSelections={setSelections}
						setChartType={setChartType}
						typeDropdownState={typeDropdownState}
						setTypeDropdownState={setTypeDropdownState}
					/>
				</Col>
				<Col md={1}></Col>
				<Col md={7}><ChartVisuals type={chartType} id={id} selections={selections} data={data} /></Col>
			</Row>
		</div><br /></>)

	return chartExample
}


const ScenariosOnOff = ({scenarioMap, scenarioTexts}) => {
	if ( ! scenarioMap) return null;
	return (<Form>
			<PropControl inline label="Scenarios" tooltip={"Toggle scenarios on/off"} 
				type='checkboxArray' prop='scenarios' options={Object.keys(scenarioMap)} 
				tooltips={scenarioTexts}
			/>
		</Form>);
};

const ChartVisuals = ({ type, id, selections, data }) => {
	let myLabels = data.columns.filter((el) => (el.indexOf("Total") == -1))
	let myDatasets = []
	selections.forEach((row, i) => {
		myDatasets.push({
			label: row[0],
			// if a row doesn't have a value, label it as a year total
			// if a row is labeled a year total, don't include it
			data: data.dataForRow[row[0]].filter((el) => (el.comment ? el.comment : "total for year").indexOf("total for year") == -1).map((el) => el.v),
			borderColor: row[1],
			backgroundColor: row[1]
		})
	})


	return (
		<div className="chart-chunk">
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
}

// what type of graph do we want
const ChartTypeDropdown = ({ setChartType, typeDropdownState, setTypeDropdownState }) => {
	return (
		<Row>
			<Dropdown isOpen={typeDropdownState} toggle={() => setTypeDropdownState(!typeDropdownState)} style={{ paddingLeft: "20px" }}>
				<DropdownToggle caret>
					Chart Type
				</DropdownToggle>
				<DropdownMenu>
					<DropdownItem onClick={() => setChartType("line")}>Line</DropdownItem>
					<DropdownItem onClick={() => setChartType("bar")}>Bar</DropdownItem>
				</DropdownMenu>
			</Dropdown>
		</Row>
	)
}

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
	console.log("ITEM TIME BABEE", pvItem)

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

	
	if (true) {
		console.log("-->", scenariosOn)
		return (
			<>
				<CSS css={`footer {display:none}`} />
				<div className='header'>
					<Row className="w-100">
						<Col md={2}><a className='mt-1 btn btn-dark'
							href={'/#sheet/' + encURI(id) + "?tab=" + (DataStore.getUrlValue("tab") || "")}>&lt; View Sheet</a></Col>
						<Col md={8}><h2>{"CHART PAGE TESTING PAGE"}</h2></Col>
					</Row>
					<Row>
						<ScenariosOnOff scenarioMap={scenarioMap} scenarioTexts={pvrun.value && pvrun.value.scenarioTexts} />
					</Row>
				</div>
				<div className="clearfix"></div>
				<div className="chart">
					<ChartChunk plandoc={plandoc} id={id} rows={rows} data={runOutput} scenarios={scenariosOn}/>
				</div>
			</>
		)
	}

};

export default ChartPage;

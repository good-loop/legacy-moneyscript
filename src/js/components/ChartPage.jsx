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



const ChartSelectionRow = ({selections, setSelections, el}) => {
	let [selectionColor, setSelectionColor] = useState({})
	let [colorPopoverOpen, setColorPopoverState] = useState(false)

	const removeSelection = (toRemove) => {
		console.log("Trying to remove ", toRemove)
		setSelections(selections.filter((el) => el[0] !== toRemove))
		console.log("???", selections, toRemove)
		delete selectionColor[toRemove]
		setSelectionColor(selectionColor)
	}

	const getSelectionColor = (selection) => {
		if (Object.keys(selectionColor).includes(selection)) return selectionColor[selection]
		return "#AAA"
	}

	const changeColor = ((el, color) => {
		selectionColor[el] = color
		setSelectionColor(selectionColor)
		console.log("!!!!!!")
		console.log(selectionColor)
		let temp = []
		temp = selections.map((pair) => {
			return ([pair[0], getSelectionColor(pair[0])])
		})
		setSelections(temp)
		console.log(">>>", temp)
	})

	const ColorPopover = (el) => (
		<Col lg={1} md={1}>
			<Button id="PopoverID" className='colorPopover' onClick={() => { setColorPopoverState(!colorPopoverOpen) }} style={{ backgroundColor: getSelectionColor(el) }}>
				Color
			</Button>
			<Popover placement='bottom' isOpen={colorPopoverOpen} target='PopoverID' toggle={() => { setColorPopoverState(!colorPopoverOpen) }}>
				<PopoverHeader>{el} color</PopoverHeader>
				<PopoverBody style={{ padding: 0, overflow: "hidden" }}>
					<Container>
						<Row>
							<Col style={{ margin: 0, paddingleft: "0 !important", paddingRight: "0 !important" }}><Button className="btn-block" onClick={() => { changeColor(el, "#95dbc6"); setColorPopoverState(!colorPopoverOpen) }} style={{ width: "100%", height: "100%", backgroundColor: "#95dbc6", borderRadius: "0", border: 0 }}></Button></Col>
							<Col style={{ margin: 0, paddingleft: "0 !important", paddingRight: "0 !important" }}><Button className="btn-block" onClick={() => { changeColor(el, "#db959e"); setColorPopoverState(!colorPopoverOpen) }} style={{ width: "100%", height: "100%", backgroundColor: "#db959e", borderRadius: "0", border: 0 }}></Button></Col>
						</Row>
						<Row>
							<Col style={{ margin: 0, paddingleft: "0 !important", paddingRight: "0 !important" }}><Button className="btn-block" onClick={() => { changeColor(el, "#9597db"); setColorPopoverState(!colorPopoverOpen) }} style={{ width: "100%", height: "100%", backgroundColor: "#9597db", borderRadius: "0", border: 0 }}></Button></Col>
							<Col style={{ margin: 0, paddingleft: "0 !important", paddingRight: "0 !important" }}><Button className="btn-block" onClick={() => { changeColor(el, "#dbb595"); setColorPopoverState(!colorPopoverOpen) }} style={{ width: "100%", height: "100%", backgroundColor: "#dbb595", borderRadius: "0", border: 0 }}></Button></Col>
						</Row>
						<Row>
							<Col style={{ margin: 0, paddingleft: "0 !important", paddingRight: "0 !important" }}><Button className="btn-block" onClick={() => { changeColor(el, "#211f1d"); setColorPopoverState(!colorPopoverOpen) }} style={{ width: "100%", height: "100%", backgroundColor: "#211f1d", borderRadius: "0", border: 0 }}></Button></Col>
							<Col style={{ margin: 0, paddingleft: "0 !important", paddingRight: "0 !important" }}><Button className="btn-block" onClick={() => { changeColor(el, "#cf72a6"); setColorPopoverState(!colorPopoverOpen) }} style={{ width: "100%", height: "100%", backgroundColor: "#cf72a6", borderRadius: "0", border: 0 }}></Button></Col>
						</Row>
					</Container>
				</PopoverBody>
			</Popover>
		</Col>
	)

	return (
	<Row style={{ margin: 15, height: "100%" }}>
		<Col lg={8} md={8}><p>{el}</p></Col>
		{ColorPopover(el[0])}
		<Col lg={1} md={1}></Col>
		<Col lg={1} md={1}><Button className='mt-1 btn btn-dark' onClick={() => removeSelection(el)}>-</Button></Col>
	</Row>)
}

// which rows are selected to be displayed
const ChartSelections = ({ rowNames, selections, setSelections }) => {
	let [searchValue, setSearchValue] = useState("")

	const addSelection = (newSelection) => {
		if (rowNames.includes(newSelection)) {
			setSelections([...selections, [newSelection, "#AAA"]]);		// TODO: This isn't a good way of finding values, dropdown / text prediction? 
			setSearchValue("")
		}
	}

	const selected = (
		<div className="selection-chunk">
			{selections.map((el) => {
				return (<ChartSelectionRow el={el} selections={selections}/>)
			})}
		</div>
	)
	return (
		<div
			className="chart-settings"
			style={{ backgroundColor: "#DDD", border: "1px solid Gray", borderRadius: "10px", padding: "2.5%" }}>
			<div className="selection-input">
				<Row>
					<Col md={8}><textarea style={{ height: "100%", width: "100%", resize: "none" }} rows="1" type="text" value={searchValue} onChange={e => setSearchValue(e.target.value)}></textarea></Col>
					<Col md={4}><Button style={{ height: "100%", width: "100%" }} className='mt-1 btn btn-dark' onClick={() => { addSelection(searchValue) }}>Add</Button></Col>
				</Row>
				<br />
				{selected}
			</div>
			<br />
		</div>
	)
}



















// settings for ChartVisual
const ChartSettings = ({ id, rowNames, selections, setSelections, setChartType, typeDropdownState, setTypeDropdownState }) => {
	console.log(rowNames)
	console.log("oooooh", selections)

	return (
		<div
			className="chart-settings"
			style={{ backgroundColor: "#EEE", border: "1px solid Gray", borderRadius: "10px", padding: "10px", height: "100%" }}>
			<ChartSelections rowNames={rowNames} selections={selections} setSelections={setSelections} />
			<br />
			<ChartTypeDropdown setChartType={setChartType} typeDropdownState={typeDropdownState} setTypeDropdownState={setTypeDropdownState} />
			<br />
		</div>
	)
}
// component for a charts setting & visuals
const ChartChunk = ({ id, rows, data }) => {
	let [selections, setSelections] = useState([])
	let [typeDropdownState, setTypeDropdownState] = useState(false)
	let [chartType, setChartType] = useState("line")

	let chartExample = (<><br />
		<div className="chart-set" style={{ height: "70vh" }}>
			<Row style={{ maxHeight: "100%" }}>
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


const ChartVisuals = ({ type, id, selections, data }) => {
	let myLabels = data.columns.filter((el) => (el.indexOf("Total") == -1))
	console.log(myLabels)
	let myDatasets = []
	selections.forEach((row) => {
		console.log("Row!, ", row)
		myDatasets.push({
			label: row[0],
			data: data.dataForRow[row[0]].filter((el) => el.comment.indexOf("total for year") == -1).map((el) => el.v),
			borderColor: row[1],
			backgroundColor: row[1]
		})
	})


	return (
		<div className="chart-chunk" style={{ height: "100%", backgroundColor: "#EEE", border: "1px solid Gray", borderRadius: "10px", padding: "10px" }}>
			<NewChartWidget
				type={type}
				responsive="true"
				maintainAspectRatio="false"
				data={
					{
						labels: myLabels,
						datasets: myDatasets
					}}
				className="test"
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

	// a chart set should contain its settings and its chart object
	let [chartSets, setChartSets] = useState([])

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

	console.log(pvItem, "OOOOOOOO")

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

	let dataForest = plandoc.sheets.map((sheet) => {
		let outputClone = cloneDeep(runOutput)
		let sheetId = sheet.id
		makeDataTree({ runOutput: outputClone, sheetId });
		console.log(sheetId, outputClone)
		return outputClone.dataTree[sheetId]
	})

	if (true) {
		return (
			<>
				<CSS css={`footer {display:none}`} />
				<div className='header'>
					<Row className="w-100">
						<Col md={2}><a className='mt-1 btn btn-dark'
							href={'/#sheet/' + encURI(id) + "?tab=" + (DataStore.getUrlValue("tab") || "")}>&lt; View Sheet</a></Col>
						<Col md={8}><h2>{"CHART PAGE TESTING PAGE"}</h2></Col>
					</Row>
				</div>
				<div className="clearfix"></div>
				<div className="chart">
					<ChartChunk id={id} rows={rows} data={runOutput} />
				</div>
			</>
		)
	}

};

export default ChartPage;

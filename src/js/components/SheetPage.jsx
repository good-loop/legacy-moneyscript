import React, { Component } from 'react';
import {ReactDOM} from 'react-dom';
import printer from '../base/utils/printer';
import C from '../C';
import Roles from '../base/Roles';
import Misc from '../base/components/Misc';
import {stopEvent, modifyHash} from '../base/utils/miscutils';
import DataStore from '../base/plumbing/DataStore';
import Settings from '../base/Settings';
import ShareWidget, {ShareLink} from '../base/components/ShareWidget';
import ListLoad, {CreateButton, ListItems} from '../base/components/ListLoad';
import ActionMan from '../plumbing/ActionMan';
import PropControl from '../base/components/PropControl';
import JSend from '../base/data/JSend';
import SimpleTable from '../base/components/SimpleTable';
import {setTaskTags} from '../base/components/TaskList';
import ServerIO from '../plumbing/ServerIO';
import ViewCharts from './ViewCharts';
import ViewSpreadSheet, { doShowMeTheMoney } from './ViewSpreadsheet';
import _ from 'lodash';
import { getPlanId } from './MoneyScriptEditorPage';
import { Alert, Col, Form, Label, Row, Button } from 'reactstrap';
import ErrAlert from '../base/components/ErrAlert';
import LinkOut from '../base/components/LinkOut';
import CSS from '../base/components/CSS';
import {crud} from '../base/plumbing/Crud';

const SheetPage = () => {
	// which plan?
	const id = getPlanId();
	if ( ! id) {
		return <Alert color='warning'>No plan ID - go to <a href='#plan'><code>plan</code></a> to select or create one</Alert>;
	}
	// load
	const type = C.TYPES.PlanDoc;
	const pvItem = ActionMan.getDataItem({type, id, status:C.KStatus.DRAFT});
	if ( ! pvItem.resolved) {
		return (<div><h1>{type}: {id}</h1><Misc.Loading /></div>);
	}
	if (pvItem.error) {
		return (<div><h1>{type}: {id}</h1><ErrorAlert error={pvItem.error} /></div>);
	}
	const item = pvItem.value;
	if (item.name) window.document.title = "M$: "+item.name;

	let _scenarios = DataStore.getUrlValue("scenarios");
	let scenariosOn = _.isString(_scenarios)? _scenarios.split(",") : _scenarios; // NB: string if fresh from url, array if modified by PropControl

	const pvrun = doShowMeTheMoney({plandoc:item, scenarios:scenariosOn});
	let scenarioMap = pvrun.value && pvrun.value.scenarios;

	// TODO use the navbar as the title bar instead of hiding it
	
	// CSS layout hack note: Getting the table to sensibly fill the bottom part of the screen using e.g. flex-column is ridiculously temperamental
	// So instead we fix the size of the above-table "header" info, and  use 100vh - that
	return <>
		<CSS css={`nav, footer {display: none !important;}
		.header {height:7em;}
		.sheet {height:calc(100vh - 7em);}
		`} />
		<div className='header'>
			<Row className="w-100">
				<Col md={6}><a className='mt-1 btn btn-dark' href={'/#plan/'+escape(id)}>&lt; View Plan</a></Col>
				<Col md={6}><h2>{item.name || item.id}</h2></Col>
			</Row>
			<ScenariosOnOff scenarioMap={scenarioMap} />
			<div className='flex-row'>
				<ImportsList runOutput={pvrun.value} />				
				{item.gsheetId && <LinkOut tooltip="see in Google Sheets" href={'https://docs.google.com/spreadsheets/d/'+item.gsheetId}>G</LinkOut>}
			</div>
		</div>
		<div className="sheet">
			<ViewSpreadSheet plandoc={item} scenarios={scenariosOn} />
		</div>
	</>;
};


const ImportsList = ({runOutput}) => {
	if ( ! runOutput || ! runOutput.imports || ! runOutput.imports.length) return null;
	// NB the import src is usually g-drive gibberish
	return <div className='ImportsList'>{runOutput.imports.map((imp,i) => <LinkOut key={imp.src} className='mr-2' href={imp.url || imp.src}>[Import {imp.name || i}]</LinkOut>)}</div>
};


const ScenariosOnOff = ({scenarioMap}) => {
	if ( ! scenarioMap) return null;
	return (<Form>
			<PropControl inline label="Scenarios" tooltip={"Toggle scenarios on/off"} type='checkboxArray' prop='scenarios' options={Object.keys(scenarioMap)} />
		</Form>);
};

SheetPage.fullWidth = true;
export default SheetPage;

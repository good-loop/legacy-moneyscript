import React, { Component } from 'react';
import {ReactDOM} from 'react-dom';
import {SJTest} from 'sjtest';
import {Login} from 'you-again';
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
import { Alert, Col, Form, Label, Row } from 'reactstrap';
import ErrorAlert from '../base/components/ErrorAlert';
import LinkOut from '../base/components/LinkOut';
import CSS from '../base/components/CSS';

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
	let scenariosOn = _scenarios? _scenarios.split(",") : null;

	const pvrun = doShowMeTheMoney({plandoc:item, scenarios:scenariosOn});
	let scenarioMap = pvrun.value && pvrun.value.scenarios;

	// TODO use the navbar as the title bar instead of hiding it
	return <>
		<CSS css={`nav {display: none !important;}`} />
		<Row>
			<Col md={6}><a className='mt-1 btn btn-dark' href={'/#plan/'+escape(id)}>&lt; View Plan</a></Col>
			<Col md={6}><h2>{item.name || item.id}</h2></Col>
		</Row>
		<ScenariosOnOff scenarioMap={scenarioMap} />
		<ViewSpreadSheet plandoc={item} scenarios={scenariosOn} />
	</>;
};

const ScenariosOnOff = ({scenarioMap}) => {
	if ( ! scenarioMap) return null;
	return (<Form inline><Label>Scenarios</Label>
		{Object.keys(scenarioMap).map(s => <PropControl tooltip={"Toggle scenario "+s} type='checkbox' prop='scenarios' value={s} label={s} key={s} className='ml-2 mr-1' />)}
		</Form>);
};

SheetPage.fullWidth = true;
export default SheetPage;

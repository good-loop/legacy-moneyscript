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
import { Alert } from 'reactstrap';

const ChartPage = () => {
	
	let dataFromLabel = {
		"Foo": {
			"2019-01-01": 1,
			"2019-02-01": 2,
			"2019-03-01": 3,
			"2019-04-01": 3,
			"2019-05-01": 2,
			"2019-06-01": 1,
		}
	};

	if (true) {
		return <><ChartWidget dataFromLabel={dataFromLabel} /></>;
	}
		
	// which plan?
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

	return <><ChartWidget dataFromLabel={dataFromLabel} /></>;
};

export default ChartPage;

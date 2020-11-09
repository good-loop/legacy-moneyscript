import React, { Component } from 'react';
import {ReactDOM} from 'react-dom';
import {SJTest} from 'sjtest';
import {Login} from 'you-again';
import _ from 'lodash';
import { Col, Row } from 'reactstrap';

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
import PropControl, {DSsetValue, standardModelValueFromInput} from '../base/components/PropControl';
import JSend from '../base/data/JSend';
import SimpleTable from '../base/components/SimpleTable';
import {setTaskTags} from '../base/components/TaskList';
import ServerIO from '../plumbing/ServerIO';
import ViewCharts from './ViewCharts';
import ViewSpreadSheet from './ViewSpreadsheet';
import SavePublishDeleteEtc from '../base/components/SavePublishDeleteEtc';
import BG from '../base/components/BG';
import AceCodeEditor from './AceCodeEditor';

/**
 * We abuse the navbar as an always-visible view switcher -- but we do want to preserve the id
 * 
 * TODO don't use focus! It's fragile. Change the urls in nav. Or dont use nav
 * 
 * @returns {?String}
 */
const getPlanId = () => {
	// which plan?
	const upath = DataStore.getValue(['location','path']);
	const type = 'PlanDoc';
	let id = upath[1];
	if ( ! id) {
		id = DataStore.getFocus(type);
		if ( ! id) {
			window.title = 'MoneyScript Planning Tool';
			return null;
		}
		let page = upath[0];
		modifyHash([page,id]);
	} else {
		// set this id as focal, so it survives page switching (which strips down the url)
		DataStore.setFocus(C.TYPES.PlanDoc, id);
	}
	const item = DataStore.getData({type, id, status:C.KStatus.DRAFT});
	let name = (item && item.name) || id;
	window.title = 'M$ plan: '+name;
	setTaskTags(type, id);
	return id;
};

const MoneyScriptEditorPage = () => {	
	let id = getPlanId();
	const type = C.TYPES.PlanDoc;
	if ( ! id) {		
		return <ListLoad type={type} status={C.KStatus.ALL_BAR_TRASH} canDelete canCreate />;
	}

	const path = DataStore.getDataPath({status:C.KStatus.DRAFT, type, id});
	const pvItem = ActionMan.getDataItem({type, id, status:C.KStatus.DRAFT});
	if ( ! pvItem.value) {
		return (<div><h1>{type}: {id}</h1><Misc.Loading /></div>);
	}
	const item = pvItem.value;
	if (item.name) window.document.title = "M$: "+item.name;
	return (
		<BG src='img/bg/data_money_82831320.jpg' fullscreen >
			<div className="MoneyScriptEditorPage">
				<Row>				
					<Col md={6}><PropControl path={path} prop="name" size="lg" /></Col>
					<Col md={6}><a className='btn btn-light' href={'/#sheet/'+escape(id)}>View SpreadSheet &gt;</a></Col>
				</Row>
				<EditScript id={id} plandoc={item} path={path} option="Text" />
				{/* <ShareLink /> */}
				{/* <ShareWidget /> */}
			</div>
		</BG>
	);
};

// const saveFn = _.debounce(({path}) => {
// 	console.warn("saveFn", path);
// 	const plandoc = DataStore.getValue(path);
// 	// parse
// 	let p = ServerIO.load('/money.json', {data: {action:'parse', text: plandoc.text}});
// 	return p.then(res => {
// 		DataStore.setValue(['transient', 'parsed', path[path.length-1]], JSend.data(res));
// 	}, res => {
// 		// error handling
// 		if (JSend.status(res) === 'fail') return JSend.data(res);
// 		throw res;
// 	});
// }, 2000);

/**
 * Ace markers -- What format?? Any docs??
 * @param {ParseFail} pf 
 */
const markerFromParseFail = pf => {
	return {startRow:pf.line, endRow:pf.line, className:'error-marker', type: 'background' };
};

const EditScript = ({id, plandoc, path}) => {
	// syntax errors?
	let parsed = plandoc; // NB: parse info is added server-side by augment
	let pes = (parsed && parsed.errors) || [];
	// standardise on tabs, with 4 spaces = 1 tab
	let modelValueFromInput = (iv, type, eventType) => standardModelValueFromInput(iv? iv.replace(/ {4}/g, '\t') : iv, type, eventType);
	return (<div>
		<AceCodeEditor path={path} prop='text' markers={pes.map(markerFromParseFail)} height="calc(100vh - 15em)" />
		<div>{parsed && parsed.errors? JSON.stringify(parsed.errors) : null}</div>
		<div>&nbsp;</div>
		<SavePublishDeleteEtc type="PlanDoc" id={id} saveAs />
	</div>);    

};

MoneyScriptEditorPage.fullWidth = true;
export default MoneyScriptEditorPage;
export {
	getPlanId
};

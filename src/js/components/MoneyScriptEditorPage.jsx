import React, { Component, useState } from 'react';
import { ReactDOM } from 'react-dom';
import _, { uniqBy } from 'lodash';
import { Col, Row, Card as BSCard, Alert, Badge, Modal, Button, ModalHeader, ModalBody, ModalFooter } from 'reactstrap';

import printer from '../base/utils/printer';
import C from '../C';
import Roles from '../base/Roles';
import Misc from '../base/components/Misc';
import { stopEvent, modifyHash, encURI, space, uniq } from '../base/utils/miscutils';
import DataStore, { getDataPath } from '../base/plumbing/DataStore';
import Settings from '../base/Settings';
import ShareWidget, { ShareLink } from '../base/components/ShareWidget';
import ListLoad, { CreateButton } from '../base/components/ListLoad';
import ActionMan from '../plumbing/ActionMan';
import PropControl, { DSsetValue, standardModelValueFromInput } from '../base/components/PropControl';
import JSend from '../base/data/JSend';
import SimpleTable from '../base/components/SimpleTable';
import LinkOut from '../base/components/LinkOut';
import { setTaskTags } from '../base/components/TaskList';
import ServerIO from '../plumbing/ServerIO';
import SavePublishDeleteEtc from '../base/components/SavePublishDeleteEtc';
import Editor3ColLayout, { MainPane, RightSidebar } from '../base/components/Editor3ColLayout';
import BG from '../base/components/BG';
import AceCodeEditor from './AceCodeEditor';
import Icon from '../base/components/Icon';
import { getStatus } from '../base/data/DataClass';
import KStatus from '../base/data/KStatus';
import PropControlList from '../base/components/PropControlList';
const dummy = PropControlList;

/**
 * @returns {?String}
 */
const getPlanId = () => {
	// which plan?
	const upath = DataStore.getValue(['location', 'path']);
	const type = 'PlanDoc';
	let id = upath[1];
	if (!id) {
		// id = DataStore.getFocus(type);
		// if ( ! id) {
		window.title = 'MoneyScript Planning Tool';
		return null;
		// }
		// let page = upath[0];
		// modifyHash([page,id]);
	}
	// else {
	// 	// set this id as focal, so it survives page switching (which strips down the url)
	// 	DataStore.setFocus(C.TYPES.PlanDoc, id);
	// }
	const item = DataStore.getData({ type, id, status: C.KStatus.DRAFT });
	let name = (item && item.name) || id;
	window.title = 'M$ plan: ' + name;
	setTaskTags(type, id);
	return id;
};

const MoneyScriptEditorPage = () => {
	let id = getPlanId();
	const type = C.TYPES.PlanDoc;
	if (!id) {
		return <ListLoad type={type} status={C.KStatus.ALL_BAR_TRASH} canDelete canCreate />;
	}

	const path = DataStore.getDataPath({ status: C.KStatus.DRAFT, type, id });
	const pvItem = ActionMan.getDataItem({ type, id, status: C.KStatus.DRAFT });
	if (!pvItem.value) {
		return (<div><h1>{type}: {id}</h1><Misc.Loading /></div>);
	}
	const item = pvItem.value;
	if (item.name) window.document.title = "M$: " + item.name;
	return (
		<BG src='img/bg/data_money_82831320.jpg' fullscreen >
			<Editor3ColLayout showAll >
				<MainPane>
					<Col md={8}><PropControl path={path} prop="name" size="lg" /></Col>
					<EditScript id={id} plandoc={item} path={path} option="Text" />
				</MainPane>
				<RightSidebar height="" overflowY="auto" >
					<a className='btn btn-primary btn-sm ml-2 mr-2' href={'/#sheet/' + escape(id)}>View SpreadSheet &gt;</a>
					<GSheetLink item={item} />
					<GitHubLink item={item} />
					<DownloadTextLink text={item.text} filename={item.name + ".txt"} />
					<BSCard className="mt-2" style={{maxWidth:"300px"}}>
						<h3>Imports</h3>
						<ImportsList cargo={item} />
						<h3>Errors</h3>
						<ErrorsList errors={item.errors} />
						<h3>Exports</h3>
						<ExportsList planDoc={item} />
					</BSCard>
					<BSCard className="mt-2" style={{maxWidth:"300px"}} >
						<SavePublishDeleteEtc size="md" type="PlanDoc" id={id} saveAs className="light" position="relative" />
					</BSCard>
					{/* <ShareLink /> */}
					{/* <ShareWidget /> */}
				</RightSidebar>
			</Editor3ColLayout>
		</BG>
	);
};

const ErrorsList = ({errors}) => {
	if ( ! errors || ! errors.length) {
		return "None :)";
	}
	return errors.map((err,i) => <Alert color='danger' key={i}>Line: {err.line} {err.message}</Alert>);
};

export const GSheetLink = ({ item }) => {
	if (!item || !item.gsheetId) {
		return null;
	}
	return (<LinkOut
		disabled={getStatus(item) !== KStatus.PUBLISHED}
		className="btn btn-light btn-sm ml-1 mr-1"
		href={'https://docs.google.com/spreadsheets/d/' + item.gsheetId}
		title={space(getStatus(item) !== KStatus.PUBLISHED && "(Publish first!)", "Link to published version in Google-Sheets")}
	><Icon size='xs' name="google-sheets" /></LinkOut>);
};

export const GitHubLink = ({ item }) => {
	if (!item) {
		return null;
	}
	return (<LinkOut
		className="btn btn-light btn-sm ml-1 mr-1"
		href={'https://github.com/good-loop/moneyscript-plans/blame/master/~' + item.id}
		title={"Link to version control in GitHub"}
	><Icon size='xs' name="github" /></LinkOut>);
};


const DownloadTextLink = ({ text, filename }) => {
	// NB the entity below is the emoji "Inbox Tray" glyph, U+1F4E5
	return (
		<a title="Download .txt" className="btn btn-light btn-sm ml-1 mr-1" href={'data:text/csv;charset=utf-8,' + encURI(text)} download={(filename || 'business-plan') + '.txt'}>
			<Icon name='.txt' />
		</a>);
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
	// markers.push({startRow: 4, startCol: 1, endRow: 4, endCol: 6, className: 'bg-danger', type: 'fullLine' })	
	// see https://github.com/securingsincity/react-ace/pull/123
	let line = pf.line
	return {
		column: 1,
		row: line,
		text: pf.message,
		type: 'error'
	};
	// return {startRow:line, startCol:1, endRow:line, endCol:2, className:'bg-warning', type: 'fullLine', text:pf.message };
};

const EditScript = ({ id, plandoc, path }) => {
	// syntax errors?
	let parsed = plandoc; // NB: parse info is added server-side by augment
	let pes = (parsed && parsed.errors) || []; //[{line:3,message:"Boo"}]; // TODO immortal error bug - as we don't reset value on return -- FIX use diffs (parsed && parsed.errors) || [];
	// standardise on tabs, with 4 spaces = 1 tab
	let modelValueFromInput = (iv, type, eventType) => standardModelValueFromInput(iv ? iv.replace(/ {4}/g, '\t') : iv, type, eventType);
	return (<div>
		<AceCodeEditor path={path} prop='text' annotations={pes.map(markerFromParseFail)} height="calc(100vh - 10em)" />
	</div>);

};

/**
 * 
 * @param {PlanDoc|Business} p.cargo can be from MoneyServlet or PlanDocServlet
 */
const ImportsList = ({cargo}) => {
	if ( ! cargo) return null;
	let imports = cargo.importCommands;	
	return <ImportsList2 verb="Import" imports={imports} />;
};

const ImportsList2 = ({verb, imports}) => {
	if ( ! imports || ! imports.length) return null;
	// filter dupes ??do this server side
	imports = uniqBy(imports, imp => imp.src);
	// NB the import src is usually g-drive gibberish
	return (<ul>
		{imports.map((imp,i) => 
			<li key={i} ><LinkOut className='mr-2' href={imp.url || imp.src}>[{imp.name || verb+" "+(i+1)}]		
				{imp.error && <Badge color="danger" title={imp.error.detailMessage || imp.error.message || JSON.stringify(imp.error)}>!</Badge>}
			</LinkOut></li>
		)}
	</ul>);
};


const ExportEditor = ({path}) => {
	// gsheet info?
	let ec = DataStore.getValue(path) || {};
	// let sheets = [];
	// if (ec.spreadsheetId) {
	// 	let pvInfo = DataStore.fetch(['widget','gsheetinfo',ec.spreadsheetId], () => {
	// 		return ServerIO.load("/gsheet/"+encURI(ec.spreadsheetId)+"?action=info");
	// 	});
	// 	if (pvInfo.value) {
	// 		sheets = pvInfo.value.sheets;
	// 		// name
	// 	}
	// }

	return (<>
		<PropControl path={path} prop="active" label="Active" type="yesNo" dflt={true} />
		<PropControl path={path} prop="name" label="Name" />		
		<PropControl path={path} prop="url" placeholder="URL" label="Google Sheet URL" type="url" required 
			help='Make a spreadsheet in Google Drive, set sharing to "anyone with the url can edit", then copy the url here' 
			saveFn={e => { /* clear id on change - server will reset it */if (ec) ec.spreadsheetId = null; } } />
		<small>ID: {ec && ec.spreadsheetId}</small> 
		{/* see GSheetsClient <PropControl path={path} prop="sheetId" label="Sheet/tab" help="Set this if you want to target a specific sheet within the spreadsheet" 
			type="select"
			labels={sheets.map(sprops => space(sprops.title, sprops.hidden&&"(hidden)"))}
			options={sheets.map(sprops => sprops.sheetId)}
	/> */}
		<PropControl path={path} prop="from" label="From" help="You can export only from a set month onwards" 
			placeholder={"e.g. Jan "+(new Date().getFullYear()+1)} />
		<PropControl path={path} prop="scenarios" label="Scenarios" type="pills" />
		<PropControl path={path} prop="overlap" label="Overlap rows only" type="yesNo" help="This is for if you setup the target export sheet with the rows and formatting you want." />
		<PropControl path={path} prop="annual" label="Annual columns" type="yesNo" />
	</>);
};

/**
 * 
 * @param {PlanDoc|Business} p.cargo can be from MoneyServlet or PlanDocServlet
 */
const ExportsList = ({planDoc}) => {
	if ( ! planDoc) return null;
	let imports = planDoc.exportCommands;
	// NB the import src is usually g-drive gibberish, so no point showing it
	const path = getDataPath({status:KStatus.DRAFT, type:C.TYPES.PlanDoc, id:planDoc.id});

	return <PropControl path={path} prop='exportCommands' type="list" Editor={ExportEditor} Viewer={ViewExport} itemType="Export to Google Sheets" />;
};

const ViewExport = ({item, i}) => {
	return <LinkOut className={space('mr-2', ! item.active && "text-muted")} href={item.url || item.src}>[{item.name || "Export "+(i+1)}]</LinkOut> 
};

MoneyScriptEditorPage.fullWidth = true;
export default MoneyScriptEditorPage;
export {
	getPlanId,
	ImportsList
};

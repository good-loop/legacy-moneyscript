import React, { Component, useState } from 'react';
import { ReactDOM } from 'react-dom';
import _, { uniqBy } from 'lodash';
import { Col, Row, Card as BSCard, Alert, Badge, Modal, Button, ModalHeader, ModalBody, ModalFooter, Nav, TabContent, NavItem, NavLink, TabPane } from 'reactstrap';

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
import { Tabs, Tab } from '../base/components/Tabs';
import PlanDoc from '../data/PlanDoc';
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
					<DownloadTextLink text={PlanDoc.text(item)} filename={item.name + ".txt"} />
					<BSCard className="mt-2" style={{ maxWidth: "300px" }}>
						<h3>Imports</h3>
						<ImportsList cargo={item} />
						<h3>Errors</h3>
						<ErrorsList errors={item.errors} sheets={item.sheets} />
						<h3>Exports</h3>
						<ExportsList planDoc={item} />
					</BSCard>
					<BSCard className="mt-2" style={{ maxWidth: "300px" }} >
						<SavePublishDeleteEtc size="md" type="PlanDoc" id={id} saveAs className="light" position="relative" sendDiff />
					</BSCard>
					{/* <ShareLink /> */}
					{/* <ShareWidget /> */}
				</RightSidebar>
			</Editor3ColLayout>
		</BG>
	);
};

const ErrorsList = ({ errors, sheets }) => {
	if (!errors || !errors.length) {
		return "None :)";
	}
	return errors.map((err, i) => <PlanError key={i} error={err} sheets={sheets} />);
};
const PlanError = ({error, sheets}) => {
	const s = sheets && sheets.find(s => s.id===error.sheetId);
	return <Alert color='danger'>{s && s.title} line: {error.line} {error.message}</Alert>;
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

/**
 * 
 * @param {Object} p
 * @param {PlanDoc} p.plandoc
 * @returns 
 */
const EditScript = ({ id, plandoc, path }) => {
	// syntax errors?
	let parsed = plandoc; // NB: parse info is added server-side by augment
	// standardise on tabs, with 4 spaces = 1 tab
	let modelValueFromInput = (iv, type, eventType) => standardModelValueFromInput(iv ? iv.replace(/ {4}/g, '\t') : iv, type, eventType);
	// backwards compatability
	if ( ! plandoc.sheets) {
		PlanDoc.addSheet(plandoc, {text:plandoc.text});
	}
	// which sheet?
	let tabId = 1*(DataStore.getUrlValue("tab") || 0);
	const deleteSheet = _tabId => {
		let ok = confirm("Are you sure you want to delete "+(plandoc.sheets[tabId].title || tabId)+"?")
		if ( ! ok) return;
		plandoc.sheets.splice(tabId, 1);
		DataStore.setValue(path, plandoc, true);
	};
	const sheet = plandoc.sheets[tabId] || {};
	let pes = (parsed && parsed.errors) || []; 
	// ...Split errors into the ones for this sheet vs others
	let pesHere = pes.filter(pf => pf.sheetId? pf.sheetId===sheet.id : true);
	console.warn("pes", pesHere);

	return (<div>
		<Nav tabs>
			{plandoc.sheets.map((sheet, i) => (<NavItem key={i} className={tabId===i? 'active' : "bg-secondary"}>
				<NavLink 
					onClick={() => DataStore.setUrlValue("tab", i)}
					className={space(tabId===i && 'active')}
				>
					{tabId===i?
						<div className='flex-row'>
							<PropControl path={path.concat('sheets', tabId)} prop='title' className="mb-0" />
							<Button color="outline-danger" size="sm" className='ml-1' onClick={e => deleteSheet(tabId)} ><Icon name="trashcan" /></Button>
						</div>
						: (sheet.title || "Sheet "+(i+1))
					}
				</NavLink>
				</NavItem>)
			)}
			<NavItem className='bg-secondary'><NavLink 
				onClick={() => {
					PlanDoc.addSheet(plandoc, {});
					DataStore.setUrlValue("tab", plandoc.sheets.length - 1);
				}}
			>+</NavLink></NavItem>
		</Nav>
		<TabContent activeTab={tabId}>
			<TabPane tabId={tabId}>
				<AceCodeEditor path={path.concat('sheets', tabId)}
					prop='text'
					annotations={pesHere.map(markerFromParseFail)} height="calc(100vh - 10em)" />
			</TabPane>
		</TabContent>
	</div>);

};

/**
 * 
 * @param {PlanDoc|Business} p.cargo can be from MoneyServlet or PlanDocServlet
 */
const ImportsList = ({ cargo }) => {
	if (!cargo) return null;
	let imports = cargo.importCommands;
	return <ImportsList2 verb="Import" imports={imports} />;
};

const ImportsList2 = ({ verb, imports }) => {
	if (!imports || !imports.length) return null;
	// filter dupes ??do this server side
	imports = uniqBy(imports, imp => imp.src);
	// NB the import src is usually g-drive gibberish
	return (<ul>
		{imports.map((imp, i) =>
			<li key={i} ><LinkOut className='mr-2' href={imp.url || imp.src}>[{imp.name || verb + " " + (i + 1)}]
				{imp.error && <Badge color="danger" title={imp.error.detailMessage || imp.error.message || JSON.stringify(imp.error)}>!</Badge>}
			</LinkOut></li>
		)}
	</ul>);
};


const ExportEditor = ({ path }) => {
	// gsheet info?
	let ec = DataStore.getValue(path) || {};

	return (<>
		<PropControl path={path} prop="active" label="Active" type="yesNo" dflt={true} />
		<PropControl path={path} prop="name" label="Name here" />
		<PropControl path={path} prop="url" placeholder="URL" label="Google Sheet URL" type="url" required
			help='Make a spreadsheet in Google Drive, set sharing to "anyone with the url can edit", then copy the url here'
			saveFn={e => { /* clear id on change - server will reset it */if (ec) ec.spreadsheetId = null; }} />
		<small>ID: {ec && ec.spreadsheetId}</small>
		{/* see GSheetsClient <PropControl path={path} prop="sheetId" label="Sheet/tab" help="Set this if you want to target a specific sheet within the spreadsheet" 
			type="select"
			labels={sheets.map(sprops => space(sprops.title, sprops.hidden&&"(hidden)"))}
			options={sheets.map(sprops => sprops.sheetId)}
	/> */}
		<PropControl path={path} prop="from" label="From" help="You can export only from a set month onwards"
			placeholder={"e.g. Jan " + (new Date().getFullYear() + 1)} />
		<PropControl path={path} prop="scenarios" label="Scenarios" type="pills" />
		<PropControl path={path} prop="overlap" label="Overlap rows only" type="yesNo" help="This is for if you setup the target export sheet with the rows and formatting you want." />
		<PropControl path={path} prop="colFreq" label="Columns" type="select"
			options={["MONTHLY_AND_ANNUAL", "ONLY_MONTHLY", "ONLY_ANNUAL"]}
			labels={["monthly and annual", "only monthly", "only annual"]} />
	</>);
};

/**
 * 
 * @param {PlanDoc|Business} p.cargo can be from MoneyServlet or PlanDocServlet
 */
const ExportsList = ({ planDoc }) => {
	if (!planDoc) return null;
	let imports = planDoc.exportCommands;
	// NB the import src is usually g-drive gibberish, so no point showing it
	const path = getDataPath({ status: KStatus.DRAFT, type: C.TYPES.PlanDoc, id: planDoc.id });

	return <PropControl path={path} prop='exportCommands' type="list" Editor={ExportEditor} Viewer={ViewExport} itemType="Export to Google Sheets" />;
};

/**
 * Just a LinkOut -- the buttons and error is done by PropControlList
 * @param {*} param0 
 * @returns 
 */
const ViewExport = ({ item, i }) => {
	return <LinkOut className={space('mr-2', !item.active && "text-muted")} href={item.url || item.src}
		>[{item.name || "Export " + (i + 1)}]</LinkOut>
};

MoneyScriptEditorPage.fullWidth = true;
export default MoneyScriptEditorPage;
export {
	getPlanId,
	ImportsList
};

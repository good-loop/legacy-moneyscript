import React, { Component, useState } from 'react';
import { ReactDOM } from 'react-dom';
import _, { uniqBy } from 'lodash';
import { Col, Row, Card as BSCard, Alert, Badge, Modal, Button, ModalHeader, ModalBody, ModalFooter, Nav, TabContent, NavItem, NavLink, TabPane } from 'reactstrap';

import printer from '../base/utils/printer';
import C from '../C';
import Roles from '../base/Roles';
import Misc from '../base/components/Misc';
import { stopEvent, modifyHash, encURI, space, uniq, urlRegex } from '../base/utils/miscutils';
import DataStore, { getDataPath, getUrlValue } from '../base/plumbing/DataStore';
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
import { getId, getName, getStatus, getType } from '../base/data/DataClass';
import KStatus from '../base/data/KStatus';
import PropControlList from '../base/components/propcontrols/PropControlList';
import { Tabs, Tab } from '../base/components/Tabs';
import PlanDoc from '../data/PlanDoc';
import MDText from '../base/components/MDText';
import { setShowLogin } from '../base/components/LoginWidget';
import { getLock } from '../base/plumbing/locker';
import Messaging from '../base/plumbing/Messaging';
import Login from '../base/youagain';
import XId from '../base/data/XId';
import _MS from './mode-ms.src';

const dummy = PropControlList || _MS;

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
		return <ListLoad type={type} status={C.KStatus.ALL_BAR_TRASH} canDelete canCreate sort='lastModified-desc' />;
	}

	let pvLock = getLock(id);
	if (pvLock.value && pvLock.value.uid !== Login.getId()) {
		Messaging.notifyUser({id:"lock"+id, text:"Being edited by "+XId.id(pvLock.value.uid), type:"error"});
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
					<RightSide plandoc={item} />
				</RightSidebar>
			</Editor3ColLayout>
		</BG>
	);
};

const RightSide = ({plandoc}) => {
	const id = getId(plandoc);
	const item = plandoc; // old code
	
	// which sheet are we on?
	let tabId = PlanDoc.getSheetIndex(plandoc);
	// links in comments?
	const text = plandoc.sheets[tabId].text || "";
	let links = [];
	const comments = text.matchAll(/[^:]\/\/.+$/gm);
	comments.forEach(c => {
		const cs = c[0];
		const urls = cs.matchAll(urlRegex).map(m => m[0]);
		urls.forEach(u => links.push(u));
	});
	links = uniq(links);

	return (<>
	<div>
		<a className='btn btn-primary btn-sm ml-2 mr-2' 
			href={'/#sheet/' + encURI(id)+"?tab="+(DataStore.getUrlValue("tab")||"")}>View Sheet &gt;</a>
		<GSheetLink item={item} />
		<GitHubLink item={item} />
		<DownloadTextLink text={PlanDoc.text(item)} filename={item.name + ".txt"} />
		<HelpLink />
		<ShareLink item={item} button color="light" size="sm" /><ShareWidget item={item} name={getName(item)} />
		<ScriptSettings className="ml-1" plandoc={plandoc} />
	</div>
	<BSCard className="mt-2" style={{ maxWidth: "300px" }}>		
		<h3>Imports
			<Misc.SubmitButton formData={{action:"clear-imports"}} size="sm" 
				className='m-auto float-right' color='secondary-outline' title='Refresh the imports'
				url={'/plandoc/'+encURI(id)} ><Icon name="reload" /></Misc.SubmitButton>
		</h3>
		<ImportsList cargo={item} />
		<h3>Errors</h3>
		<ErrorsList errors={item.errors} sheets={item.sheets} />
		<h3>Exports</h3>
		<ExportsList planDoc={item} />
		{/* <Misc.SubmitButton size="sm"			
			url='/money?action=export'
			title='Exports are normally done when you publish or re-publish'>Export Now
		</Misc.SubmitButton> */}
		<h3>Comment Links</h3>
		<ul>
		{links.map(link => <li key={link}><LinkOut href={link} fetchTitle /></li>)}
		</ul>
	</BSCard>
	<BSCard className="mt-2" style={{ maxWidth: "300px" }} >
		<SavePublishDeleteEtc size="md" type="PlanDoc" id={id} className="light" position="relative" 
			canDiscard saveAs
			sendDiff={true /* TODO this being on is not the culprit for the flickering edits bug */} />
	</BSCard></>);
	// {/* <ShareLink /> */}
	// {/* <ShareWidget /> */}
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
		href={'https://github.com/good-loop/moneyscript-plans/blame/master/PlanDoc/~' + item.id+".txt"}
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



const HelpLink = () => {
	let [open, setOpen] = useState();
	let helpText = "";
	if (open) {
		let pvHelpText = DataStore.fetch(["misc", "help"], () => {
			// TODO offer other help pages? an index?
			return ServerIO.load("/doc/examples.md");
		});
		helpText = pvHelpText.value || "...";
	}
	return (<>
		<Button title="Help" className="btn btn-light btn-sm ml-1 mr-1" onClick={e => stopEvent(e) && setOpen(true)}>
			<Icon name='help' />
		</Button>
		<Modal
				isOpen={open}
				className="help-modal"
				toggle={() => setOpen( ! open)}
				size="lg"
			>
				<ModalHeader toggle={() => setOpen( ! open)}>
					Help
				</ModalHeader>
				<ModalBody>
					<MDText source={helpText} />
				</ModalBody>
			</Modal>
		</>		
		);
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
	let tabId = PlanDoc.getSheetIndex(plandoc);
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
	
	
	let clickHandlerAdded = false;

	// on ctrl + click on a url, open that page
	const ctrlClickLink = (e) => {
		if ( ! e.cursor || ! e.session) return; // paranoia
		let row = e.cursor.row
		let col = e.cursor.column
		let token = e.session.getTokenAt(row, col)
		
		// string.unquoted are only urls
		if (token?.type !== 'string.unquoted') {
			return;
		}
		if (clickHandlerAdded) return;

		// add and remove the handler, fixes weird bug that was due to the onSelectionChange
		// being called on the click down and the click up
		const clickHandler = (e) => {
			document.body.removeEventListener('click', clickHandler);
			clickHandlerAdded = false;
			if (e.ctrlKey && token.value) {
				window.open(token.value)
			}
		};
		document.body.addEventListener('click', clickHandler);
		clickHandlerAdded = true;
	}
	
	// auto complete row names TODO how to make this refresh?
	let completions = plandoc.rowNames?.map(rn => {return {value:rn};});
	if (completions) { // HACK add keywords
		if (window.ms_keywords) completions.push(...ms_keywords.split("|"));
		if (window.ms_builtInConstants) completions.push(...ms_builtInConstants.split("|"));
		if (window.ms_builtInFunctions) completions.push(...ms_builtInFunctions.split("|"));
	}
	// console.log("completons for ",completions, plandoc);

	return (<div>
		<Nav tabs>
			{plandoc.sheets.map((sheet, i) => (<NavItem key={i} className={tabId===i? 'active' : "bg-secondary"}>
				<NavLink 
					onClick={() => DataStore.setUrlValue("tab", i)}
					className={space(tabId===i && 'active')}
				>
					{tabId===i?
						<div className='flex-row'>
							<PropControl path={path.concat('sheets', tabId)} prop='title' className="mb-0" warnOnUnpublished={false} />
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
					completions={completions}
					prop='text'
					annotations={pesHere.map(markerFromParseFail)} 
					height="calc(100vh - 10em)"
					onSelectionChange={ctrlClickLink}
				/>
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
	const ec = DataStore.getValue(path) || {};

	return (<>
		<PropControl path={path} prop="active" label="Active" type="yesNo" dflt={true} />
		<p><small>Last run: <Misc.RelativeDate date={ec.lastGoodRun} /></small></p>
		<PropControl path={path} prop="name" label="Name here" />
		<PropControl path={path} prop="url" placeholder="URL" label="Google Sheet URL" type="url" required
			help='Make a spreadsheet in Google Drive, set sharing to "anyone with the url can edit", then copy the url here'
			saveFn={e => { /* clear id on change - server will reset it */ ec.spreadsheetId = null; }} />
		<small>ID: {ec.spreadsheetId}</small>
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
		<PropControl path={path} prop="preferFormulae" label="Formulae" type="checkbox" help="Export formulae instead of numbers where possible" />
		<PropControl path={path} prop="comments" label="Export comments" type="checkbox" help='Include comments within formulae using N("comment")' />
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
	return <><LinkOut className={space('mr-2', !item.active && "text-muted")} href={item.url || item.src}
		>[{item.name || "Export " + (i + 1)}]</LinkOut><small>Last run: <Misc.RelativeDate date={item.lastGoodRun} /></small></>
};

const ScriptSettings = ({plandoc, color="light", className}) => {
	const [show, setShow] = useState();
	const toggle = () => setShow( ! show);
	const path = getDataPath({id:plandoc.id,type:C.TYPES.PlanDoc,status:KStatus.DRAFT});
	const spath = path.concat("settings");
	return (<>
	<Button title="Settings" onClick={toggle} size="sm" color={color} className={className}><Icon name="settings" /></Button>
	<Modal isOpen={show} toggle={toggle}>
		<ModalHeader toggle={toggle}>
			<Icon name="settings" /> Sheet Settings
		</ModalHeader>
		<ModalBody>
			<PropControl type="select" prop="numberFormat" label="Number format" options={["abbreviate","standard"]} 
				labels={["abbreviations e.g. £10k", "standard e.g. £10,000"]} path={spath}/>
		</ModalBody>
		</Modal>
		</>);
};


MoneyScriptEditorPage.fullWidth = true;
export default MoneyScriptEditorPage;
export {
	getPlanId,
	ImportsList
};

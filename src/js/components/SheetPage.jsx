import _ from 'lodash';
import React from 'react';
import { Alert, Col, Form, Row } from 'reactstrap';
import CSS from '../base/components/CSS';
import ErrAlert from '../base/components/ErrAlert';
import Icon from '../base/components/Icon';
import LinkOut from '../base/components/LinkOut';
import Misc from '../base/components/Misc';
import PropControl from '../base/components/PropControl';
import DataStore from '../base/plumbing/DataStore';
import C from '../C';
import ActionMan from '../plumbing/ActionMan';
import { getPlanId, GSheetLink } from './MoneyScriptEditorPage';
import ViewSpreadSheet, { doShowMeTheMoney } from './ViewSpreadsheet';

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
		return (<div><h1>{type}: {id}</h1><ErrAlert error={pvItem.error} /></div>);
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
			<ScenariosOnOff scenarioMap={scenarioMap} scenarioTexts={pvrun.value && pvrun.value.scenarioTexts} />
			<div className='flex-row'>
				<ImportsList runOutput={pvrun.value} />			
				<GSheetLink item={item}	/>
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


const ScenariosOnOff = ({scenarioMap, scenarioTexts}) => {
	if ( ! scenarioMap) return null;
	return (<Form>
			<PropControl inline label="Scenarios" tooltip={"Toggle scenarios on/off"} 
				type='checkboxArray' prop='scenarios' options={Object.keys(scenarioMap)} 
				tooltips={scenarioTexts}
			/>
		</Form>);
};

SheetPage.fullWidth = true;
export default SheetPage;

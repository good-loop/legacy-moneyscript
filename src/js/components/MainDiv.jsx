import React, { Component } from 'react';

// Plumbing
import DataStore from '../base/plumbing/DataStore';
import C from '../C';
// Templates
import LoginWidget from '../base/components/LoginWidget';
import MessageBar from '../base/components/MessageBar';
import NavBar from '../base/components/NavBar';
// Pages
import {BasicAccountPage} from '../base/components/AccountPageWidgets';
import TestPage from '../base/components/TestPage';
import AccountMenu from '../base/components/AccountMenu';
import MSEditorPage from './MoneyScriptEditorPage';

import SheetPage from './SheetPage';
import ChartPage from './ChartPage';
import TaskList from '../base/components/TaskList';
import Crud from '../base/plumbing/Crud';
import MainDivBase from '../base/components/MainDivBase';
import PropControls from '../base/components/PropControls';

C.setupDataStore();

const PAGES = {
	plan: MSEditorPage,
	sheet: SheetPage,
	chart: ChartPage,
	account: BasicAccountPage,
	test: TestPage
};

/**
		Top-level: tabs
*/
const MainDiv = () => {
	return <MainDivBase loginRequired navbarPages={['plan']} defaultPage='plan' pageForPath={PAGES} fullWidthPages={['sheet']} />;
};

export default MainDiv;

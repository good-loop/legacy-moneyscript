import React, { Component } from 'react';
import {ReactDOM} from 'react-dom';

//
// import "ace-builds/src-noconflict/mode-java";
// import "ace-builds/src-noconflict/theme-github";


// import brace from 'brace';
// import AceEditor from 'react-ace';
// import 'brace/mode/java';
// import 'brace/theme/github';
	// TODO https://ace.c9.io/#nav=embedding or https://codemirror.net/
	// or https://microsoft.github.io/monaco-editor/

import AceEditor from "react-ace";
import PropControl, {DSsetValue} from '../base/components/PropControl';

// ?? import webpack resolver to dynamically load modes, you need to install file-loader for this to work!
// import "../../build/webpack-resolver";


// TODO control-f is broken :( It DOES work in the react-ace demo -- something in the webpack setup??

ace.config.set('basePath','/lib/ace')

const AceCodeEditor = ({path, prop, markers, ...props}) => {
	// DEBUG
	if ( ! markers) markers = [];
	// markers.push({startRow: 3, startCol: 5, endRow: 4, endCol: 6, className: 'replacement_marker', type: 'text' })	
	// markers.push({startRow: 1, startCol: 1, endRow: 2, endCol: 6, className: 'bg-warning', type: 'fullLine' })	

	// markers.push({startRow: 4, startCol: 1, endRow: 4, endCol: 6, className: 'bg-danger', type: 'fullLine' })	

	return <div className='position-relative'><AceEditor
		{...props}
	width="100%"
	placeholder=""
	mode="json" //acems" // previously tried json
	theme="tomorrow"
	name="planit1"
	onLoad={editor => console.log("Ace onLoad")}
	onChange={newText => DSsetValue(path.concat(prop), newText, true)}
	fontSize={16}
	showPrintMargin={false}
	showGutter
	highlightActiveLine
	value={DataStore.getValue(path.concat(prop))}
	setOptions={{
		// to use this options the corresponding extension file needs to be loaded in addition to the ace.js
		// enableBasicAutocompletion: false,
		// enableLiveAutocompletion: true,
		// enableSnippets: false,
		showLineNumbers: true,
		tabSize: 4,
	}}
	// control-f find is broken??
	// editorProps={{
	// 	editor.commands.addCommand({
	// 		name: "unfind",
	// 		bindKey: {
	// 			win: "Ctrl-F",
	// 			mac: "Command-F"
	// 		},
	// 		exec: function(editor, line) {
	// 			return false;
	// 		},
	// 		readOnly: true
	// 	})
	// }}
	// markers={markers} Not working ?!
	/>
	{markers.map(m => <div key={JSON.stringify(m)} className='bg-warning' style={{position:"absolute",left:"75%",right:0,top:16*m.startRow}} title={m.text}>{m.text}</div>)}
	</div>
};	

export default AceCodeEditor;

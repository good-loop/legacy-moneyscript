import React, { Component } from 'react';
import {ReactDOM} from 'react-dom';

import AceEditor from "react-ace";
import PropControl, {DSsetValue} from '../base/components/PropControl';
import CSS from '../base/components/CSS';

import "ace-builds/src-noconflict/ext-prompt";
import "ace-builds/src-noconflict/ext-searchbox";
import "ace-builds/src-noconflict/ext-language_tools";
// import "ace-builds/src-noconflict/theme-tomorrow";
// import "ace-builds/src-noconflict/theme-tomorrow_night_eighties";
// import "ace-builds/webpack-resolver";

/**
 * HACK! In order to get our mode to load - we hacked the ace-build python mode file!
 * Our file mode-ms.src.js should be copied into place over node_modules/ace-builds/src-noconflict/mode-python.js
 * This is done by the package.json compile script
 */
import "ace-builds/src-noconflict/mode-python";


// TODO control-f is broken :( It DOES work in the react-ace demo -- something in the webpack setup??
/**
 * 
 * @param {Object} p
 * @param {Object[]} p.completions [{value: "word"}]);
 * @returns 
 */
const AceCodeEditor = ({path, prop, annotations, markers, completions, ...props}) => {
	// DEBUG
	// if ( ! markers) markers = [];
	// markers.push({startRow: 6, startCol: 1, endRow: 6, endCol: 2, className: 'wibble', type: 'fullLine' })	
	// markers.push({startRow: 4, startCol: 1, endRow: 4, endCol: 6, className: 'bg-danger', type: 'fullLine' })	

	// https://github.com/ajaxorg/ace/wiki/How-to-enable-Autocomplete-in-the-Ace-editor
	// https://github.com/securingsincity/react-ace/issues/69
	// TODO make this refresh somehow
	let customCompleter = completions && {
		getCompletions: function(editor, session, pos, prefix, callback) {
			 callback(null, completions);
		}
   };
//   langTools.addCompleter(customCompleter);

	return <div className='position-relative'>
		<CSS css={`.wibble {background:green; position:absolute;}`}/>
		<AceEditor
		{...props}
	width="100%"
	placeholder=""
	mode="python" //acems" // previously tried json
	// theme="tomorrow_night_eighties"
	name="planit1"
	onLoad={editor => console.log("Ace onLoad")}
	onChange={newText => DSsetValue(path.concat(prop), newText, true)}
	fontSize={17}
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
	markers={markers} //Not working ?!
	annotations={annotations}
	enableBasicAutocompletion={customCompleter? [customCompleter] : true}
	enableLiveAutocompletion={false} /* auto popup as you type - off 'cos doesnt handle numbers well */
	/>
	{/* {markers.map(m => <div key={JSON.stringify(m)} className='bg-warning' style={{position:"absolute",left:"75%",right:0,top:16*m.startRow}} title={m.text}>{m.text || "!"}</div>)} */}
	</div>
};	

export default AceCodeEditor;

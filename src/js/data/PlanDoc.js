const { default: DataClass, nonce } = require("../base/data/DataClass");

class PlanDoc extends DataClass {

}
DataClass.register(PlanDoc, "PlanDoc");
export default PlanDoc;

/**
 * 
 * @param {!PlanDoc} plandoc 
 * @returns {!String} Joins the sheets into a text blob
 */
PlanDoc.text = plandoc => {
	PlanDoc.assIsa(plandoc);
	if ( ! plandoc.sheets) {
		return plandoc.text;
	}
	let text = plandoc.sheets.map(sheet => sheet.text).join("\n\n");
	return text;
};

PlanDoc.addSheet = (plandoc, sheet={}) => {
	PlanDoc.assIsa(plandoc);
	if ( ! plandoc.sheets) plandoc.sheets = [];
	if ( ! sheet.text) sheet.text = "";
	if ( ! sheet.id) sheet.id = nonce();
	if ( ! sheet.title) sheet.title = "Sheet "+(plandoc.sheets.length + 1);
	plandoc.sheets.push(sheet);
	return sheet;
}

/** 
 * Wrapper for server calls.
 *
 */
import _ from 'lodash';
import $ from 'jquery';
import C from '../C';
import ServerIO from '../base/plumbing/ServerIOBase';


ServerIO.APIBASE = 'https://moneyscript.good-loop.com'; // live data, edited local!

ServerIO.DATALOG_ENDPOINT = 'https://testlg.good-loop.com/data';
ServerIO.DATALOG_ENDPOINT = '/data';

export default ServerIO;


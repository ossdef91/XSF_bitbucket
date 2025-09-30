
///////////////////////////////////////////////////////////////////////////////
// (c) Dassault Systemes, 1993 - 2017.  All rights reserved.
//
// This file contains helper JavaScript functions used by the FileSys Connector
// Configuration panel.
// These functions may not currently be used. This is provided as an example
// of how JS functions can be defined to be called from the page that is
// generated through the JSP code on the server.
//
///////////////////////////////////////////////////////////////////////////////
define('DS/scmfilesys/scmfilesys', [], function() {
	'use strict';
	var scmfilesys = {
		/*** Trim leading and trailing spaces from a string.
		 *
		 * @param {string} str The string to trim
		 * @return {string} The trimmed string
		 */
		trimString: function (str) {
		    while (str.charAt(0) === ' ') {
		        str = str.substring(1);
		    }
		    while (str.charAt(str.length - 1) === ' ') {
		        str = str.substring(0, str.length - 1);
		    }
		    return str;
		},
		
		/*** Display an error message in a popup.
		 * @param {string} msg The message
		 */
		showMessage: function (msg) {
			alert(msg);
		},
		
		/*** Validation for the connector configuration form
                 * Here we could put any checks on the form values.
		 *
		 * @param {string} f Name of the form
		 * @return {boolean} Indicates if form contents are valid
		 */
		Validate2: function ( f ) {
	            return true;
		},
	
	
		/***
		 * Adds a hidden variable to a form.
		 * @param {string} theForm Name of the form
		 * @param {string} name Name of the field to add
		 * @param {string} value Value of the field
		 */
		addHidden: function (theForm, name, value) {
			var field = document.createElement('input');
		    field.type = 'hidden';
		    field.name = name;
		    field.value = value;
		    theForm.appendChild(field);
		}
		
	}
	window.scmfilesys = scmfilesys;
	return scmfilesys;
});


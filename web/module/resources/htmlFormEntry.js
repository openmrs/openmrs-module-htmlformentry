var tempRadioStatus;

// remember the status of this radio button when it is first clicked, for use in the radioClicked method
function radioDown(radioButton) {
	tempRadioStatus = radioButton.checked;
}

// if the radio was already checked when we clicked on it, then uncheck it
function radioClicked(radioButton) {
	if (tempRadioStatus) {
		radioButton.checked = false;
	}
}

function showError(errorDivId, errorMessage) {
	var errorDiv = document.getElementById(errorDivId);
	if (errorDiv == null) {
		window.alert("Error: " + errorMessage + "\n\n(Cannot find div: " + errorDivId + ")");
	} else {
		if (errorDiv.innerHTML != '')
			errorDiv.innerHTML += ', ' + errorMessage;
		else
			errorDiv.innerHTML = errorMessage;
		showDiv(errorDivId);
	}
}

function clearError(errorDivId) {
	hideDiv(errorDivId);
	var errorDiv = document.getElementById(errorDivId);
	if (errorDiv != null) {
		errorDiv.innerHTML = '';
	}
}

function checkNumber(el, errorDivId, floatOkay, absoluteMin, absoluteMax) {
	clearError(errorDivId);
	
	if (el.value == '') {
		el.className = null;
	}
		
	var errorMessage = verifyNumber(el, floatOkay, absoluteMin, absoluteMax);
	if (errorMessage == null) {
		el.className = 'legalValue';
		clearError(errorDivId);
	} else {
		el.className = 'illegalValue';
		showError(errorDivId, errorMessage);
	}
}

function verifyNumber(el, floatOkay, absoluteMin, absoluteMax) {
	var val = el.value;
	if (val == '')
		return null;

	// TODO replace parse* functions with something that catches 12a.
	if (floatOkay) {
		val = parseFloat(val);
	} else {
		val = parseInt(val);
	}
	
	if (isNaN(val)) {
		if (floatOkay)
			return "Not a number";
		else
			return "Not an integer";
	}
		
	if (absoluteMin != null) {
		if (val < absoluteMin)
			return "Cannot be less than " + absoluteMin;
	}
	if (absoluteMax != null) {
		if (val > absoluteMax)
			return "Cannot be greater than " + absoluteMax;
	}
	return null;
}

function getElementsByClass(node, searchClass) {
	var ret = new Array();
	var els = node.getElementsByTagName('*');
	for (var i = 0; i < els.length; ++i) {
		if (els[i].className == searchClass)
			ret.push(els[i]);
	}
	return ret;
}

function findParentWithClass(anElement, className) {
	var parent = anElement.parentNode;
	while (parent != null && parent.className != className)
		parent = parent.parentNode;
	return parent;
}

function showAnotherRow(tableId) {
	var className = 'repeating_table_' + tableId;
	var els = getElementsByClass(document, className);
	for (var i = 0; i < els.length; ++i) {
		if (els[i].style.display == 'none') {
			// display it
			els[i].style.display = '';
			// set its hidden flag to say it's visible
			var flags = getElementsByClass(els[i], 'visibleFlag');
			if (flags.length > 0)
				flags[0].value = 'true'
			return;
		}
	}
}

function removeRow(tableId, anElementInRow) {
	var className = 'repeating_table_' + tableId;
	var row = findParentWithClass(anElementInRow, className);
	if (row) {
		row.style.display = 'none';
		var flags = getElementsByClass(row, 'visibleFlag');
		if (flags.length > 0)
			flags[0].value = 'false';
	}
}

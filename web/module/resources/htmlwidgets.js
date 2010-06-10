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

function findParentWithClass(element, parentClass) {
	var parent = element.parentNode;
	while (!$(parent).hasClass(parentClass)) {
		parent = parent.parentNode;
	}
	return parent;
}

function removeParentWithClass(element, parentClass) {
	var parent = findParentWithClass(element, parentClass);
	$(parent).remove(); 
}

function getClone(idToClone) {
	var template = $("#"+idToClone);
	var c = $(template).clone(true);
	$(c).show();
	return c;
}

function cloneAndInsertBefore(idToClone, elementToAddBefore) {
	var newRow = getClone(idToClone);
	$(newRow).insertBefore(elementToAddBefore);
	return newRow;
}

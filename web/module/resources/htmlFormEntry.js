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

/* from htmlwidgets.js */

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

/*add for remove span in repeat*/

function removeParentWithClass(element, parentClass) {
	var parent = findParentWithClass(element, parentClass);
	$(parent).remove(); 
	updateAllParent(parent.id);
	//newRepeat1_1
	var pos = parent.id.indexOf("_");
	var temp = parent.id.substring(9, pos);
	$('#kCount'+temp).val($('#kCount'+temp).val()-1)
}

function updateAllParent(elementid){

	var nextId = GetNewRptTimeId(elementid, 1);
	
	while($("#"+nextId).length > 0 ){
		updateRepeatSpan(nextId);
		nextId = GetNewRptTimeId(nextId, 1);
	}
}

function updateRepeatSpan(rptspanid){
	var id = rptspanid;
	var element = $("#"+rptspanid)[0];
	var nextId = GetNewRptTimeId(element.id, -1);
	element.id = nextId;
	
	for(var i = 0; i< element.children.length;++i){
		var child = element.children[i];
		if(child.id!="")
			updateChildren(child.id);
	}
}


function updateChildren(childid){
	if(childid =="")return;
	var child = $("#"+childid)[0];
	var newid = GetNewRptTimeId(child.id, -1)
	child.id = newid;
	child.name = newid;
	if(child.attributes["onBlur"]!== undefined){
	    var onblur =child.attributes["onBlur"].value;
		pos1 = onblur.indexOf("'");
		pos2 = onblur.indexOf("'", pos1+1);
		var temp =  onblur.substring(pos1+1,pos2);
		pos1 = child.id.indexOf("_");
		var tmp = child.id.substring(3,pos1);
		temp = onblur.replace(temp, GetNewRptTimeId(temp, -1));
		child.attributes["onBlur"].value = temp;
	}
	//if(child.attribute("onblur")!=null)
	//deal with onblur to correct the error field
	for(var i = 0; i< child.children.length; ++i){
		var children = child.children[i];
		updateChildren(children.id);
	}
}

function GetNewRptTimeId(id, offset){
	var index = id.indexOf("_");
	if(index == -1)//should be like newrepeat1
	{
		return id;
	}
	
	if(id.indexOf("_", index+1) != -1)
		index  = id.indexOf("_", index+1);
	
	var rpttime = parseInt(id.substring(index+1,id.length));
	var offset = parseInt(offset);
	id = id.substring(0,index+1)+(rpttime+offset);
	//alert(id);
	return id;
}



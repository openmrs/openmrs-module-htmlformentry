var tempRadioStatus;

// remember the status of this radio button when it is first clicked, for use in
// the radioClicked method
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
		window.alert("Error: " + errorMessage + "\n\n(Cannot find div: "
				+ errorDivId + ")");
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
	for ( var i = 0; i < els.length; ++i) {
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
	for ( var i = 0; i < els.length; ++i) {
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

/* from htmlwidgets.js and new js */

function getClone(idToClone) {
	var template = $("#" + idToClone);
	var c = $(template).clone(true);
	$(c).show();
	return c;
}

function cloneAndInsertBefore(idToClone, elementToAddBefore) {
	var newRow = getClone(idToClone);
	$(newRow).insertBefore(elementToAddBefore);
	return newRow;
}

/* add for remove span in repeat */

function removeParentWithClass(element, parentClass) {
	var parent = findParentWithClass(element, parentClass);
	var pos = parent.id.indexOf("_");
	var temp = parent.id.substring(9, pos);
	var newKCount = $('#kCount' + temp).val() - 1;
	var minRpt = $('#minRpt' + temp).val();
	
	if(newKCount >=minRpt ){
		$(parent).remove();
		updateAllParent(parent.id);
		// newRepeat1_1
		$('#kCount'+temp).val(newKCount);
		// $('#newRepeat'+temp+'_'+newKCount).css("display", "inline");
	}else{
		alert("Multiple minimum reached, please contact form designer");
	}
}

function updateAllParent(elementid) {

	var nextId = GetNewRptTimeId(elementid, 1);

	while ($("#" + nextId).length > 0) {
		updateRepeatSpan(nextId);
		nextId = GetNewRptTimeId(nextId, 1);
	}
}

function updateRepeatSpan(rptspanid) {
	var id = rptspanid;
	var element = $("#" + rptspanid)[0];
	var nextId = GetNewRptTimeId(element.id, -1);
	element.id = nextId;

	for ( var i = 0; i < element.children.length; ++i) {
		var child = element.children[i];
		// if(child.id!="")
		updateChildren(child);
	}
}

function updateChildren(child) {
	if (child.id != "") {
		// var child = $("#"+childid)[0];
		var newid = GetNewRptTimeId(child.id, -1)
		child.id = newid;
		child.name = newid;
		if (child.attributes["onBlur"] !== undefined) {
			var onblur = child.attributes["onBlur"].value;
			pos1 = onblur.indexOf("'");
			pos2 = onblur.indexOf("'", pos1 + 1);
			var temp = onblur.substring(pos1 + 1, pos2);
			pos1 = child.id.indexOf("_");
			var tmp = child.id.substring(3, pos1);
			temp = onblur.replace(temp, GetNewRptTimeId(temp, -1));
			child.attributes["onBlur"].value = temp;
		}
	}
	for ( var i = 0; i < child.children.length; ++i) {
		updateChildren(child.children[i]);
	}
}

function GetNewRptTimeId(id, offset) {
	var index = id.indexOf("_");
	if (index == -1)// should be like newrepeat1
	{
		return id;
	}

	if (id.indexOf("_", index + 1) != -1)
		index = id.indexOf("_", index + 1);
	
	var dotIndex = id.indexOf(".");
	if( dotIndex == -1){
		var rpttime = parseInt(id.substring(index + 1, id.length));
		var offset = parseInt(offset);
		id = id.substring(0, index + 1) + (rpttime + offset);
	}else{
		var rpttime = parseInt(id.substring(index + 1, dotIndex+1));
		id = id.substring(0, index + 1) + (rpttime + offset)+id.substring(dotIndex,id.length);
	}
	// alert(id);
	return id;
}


function addNewMutipleGroup(rpti,addbtm){
	
	/* the jquery function to add fields */
	var kCount = parseInt($('#kCount'+ rpti ).val()) + 1;
    var maxRpt = parseInt($('#maxRpt'+ rpti ).val());
	
    if(kCount>maxRpt){alert('Multiple Limit reached, please contact the form designer!');return;}

	$('#kCount' + rpti).val(kCount);
	var $newRow = cloneAndInsertBefore('newRepeatTemplate'+rpti, addbtm);
	
	$('#newRepeat' + rpti + "_"+ (kCount-1)).css("display", "block");
	$newRow.attr('id', 'newRepeat'+ rpti + "_" + kCount);
	$newRow.css("display", "block");
	// sb.append("$newRow.prepend('<br/>'); \n");

	var newRowChildren = $newRow.children();

	var stack = new Array();
	for(var i = 0; i< newRowChildren.length; ++i){
		stack.push(newRowChildren[i]);
	}

	while(stack.length> 0){
		var child = stack.pop();
		for(var i = 0; i< child.children.length; ++i){
			stack.push(child.children[i]);
		}

		if (child.id == 'removeRowButton') { child.style.display = ''; }
		else if(child.id =="defaultFieldlistObjAddButton0"){child.style.display = 'none'};
	
		if (child.className == 'error')
		{ 
			child.style.display = 'none'; 
		}	

		if(child.id.length>4 && child.id.substring(0,3)== 'rpt'){
			var pos1 = child.id.indexOf("_");
			var pos2 = child.id.indexOf("_", pos1+1);

			if(child.id.indexOf(".")==-1){
				child.id = child.id.substring(0,pos2+1) + kCount;
				child.name = child.id;
			}
			else{
				child.id = child.id.substring(0, pos2 + 1) + kCount +child.id.substring(child.id.indexOf("."),child.id.length);
				child.name = child.id;
			}

			if(child.attributes["onBlur"]!== undefined){
				var onblur =child.attributes["onBlur"].value;
				pos1 = onblur.indexOf("'");
				pos2 = onblur.indexOf("'", pos1+1);
				var temp =  onblur.substring(pos1+1,pos2);
				temp = onblur.replace(temp, GetNewRptTimeId(temp, $('#kCount'+ rpti).val()));
				child.attributes["onBlur"].value=temp;
			}
		}
	}
}
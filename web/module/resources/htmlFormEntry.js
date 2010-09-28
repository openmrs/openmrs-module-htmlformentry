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

function setupAutocomplete(element,src, answerids, answerclasses) {
	var hiddenField = jQuery("#"+element.id+"_hid");
	var textField = jQuery(element);
	var select = false;
	
	 /*
	 * There are only 2 valid cases:
	 * 1) user type something,then selected an item from the suggestion list, then leave.
	 * 2) user leave the input field with the input field empty.
	 * otherwise, we willl trigger the error
	 * The flow is, user typed in sth. ,if got suggestion and user clicked an item
	 * it will trigger the select event, then close event in this function.
	 * finaly it will trigger the onblur when user leave this input box.
	 */
	if (hiddenField.length > 0 && textField.length > 0) {
		textField.autocomplete( {
			 source: function(req, add){  
	        //pass request to server  
			jQuery.getJSON(src+'?answerids='+answerids+'&answerclasses='+answerclasses, req, function(data) {  
				   
			//create array for response objects  
			var suggestions = [];  
				   
			jQuery.each(data, function(i, val){  
			suggestions.push(val);  
		   });
		   
		   	//this clears the error if it returns no result
		    //if the input field is not empty
		    //the error will be triggered in onblur below
			if (suggestions.length==0) hiddenField.val("");
			
			add(suggestions);  
		 });  
		}
			,
			minLength: 2,
			select: function(event, ui) {
					hiddenField.val(ui.item.id);
					select = true;
			},
		    close: function(event, ui) {
				if(select)//user has selected item from the list
					textField.css('color', 'green');
				else {	
					textField.css('color', 'red');
					hiddenField.val("ERROR");
				}
				select = false;
			}
		});
	} 	
}

function onBlurAutocomplete(element){
	var hiddenField = jQuery("#"+element.id+"_hid");
	var textField = jQuery(element);
	
	if(textField.val() != "" && (hiddenField.val()==""||hiddenField.val()=="ERROR")){
		hiddenField.val("ERROR");
		textField.css('color', 'red');
	}
}


function getField(elementAndProperty) {
	var info = propertyAccessorInfo[elementAndProperty];
	if (info) {
		var widgetId = info.id;
		var fn = info.field;
		if (fn == null)
			fn = defaultFieldFunction;
		var tmp = fn(widgetId);
		return tmp;
	} else {
		return null;
	}	
}


function getValue(elementAndProperty) {
	var info = propertyAccessorInfo[elementAndProperty];
	if (info) {
		var widgetId = info.id;
		var fn = info.getter;
		if (fn == null)
			fn = defaultGetterFunction;
		return fn(widgetId);
	} else {
		return null;
	}
}

function setValue(elementAndProperty, value) {
	var info = propertyAccessorInfo[elementAndProperty];
	if (info) {
		var widgetId = info.id;
		var fn = info.setter;
		if (fn == null)
			fn = defaultSetterFunction;
		return fn(widgetId, value);
	} else {
		window.alert("Form scripting error: no property accesser info available for " + elementAndProperty);
	}
}

function defaultFieldFunction(widgetId) {
	return jQuery('#' + widgetId);
}

function defaultGetterFunction(widgetId) {
	return DWRUtil.getValue(widgetId);
}

function defaultSetterFunction(widgetId, value) {
	if (widgetId == null || document.getElementById(widgetId) == null) {
		window.alert("Form scripting error: cannot find widget " + widgetId);
		return;
	}
	DWRUtil.setValue(widgetId, value);
}

function checkboxGetterFunction(widgetId) {
	var sel = DWRUtil.getValue(widgetId);
	if (sel) {
		return jQuery('#' + widgetId).val();
	} else {
		return null;
	}
}

// turns the checkbox on if you pass it (boolean) true or the 'value' field of the checkbox element
function checkboxSetterFunction(widgetId, value) {
	if (widgetId == null || document.getElementById(widgetId) == null) {
		window.alert("Form scripting error: cannot find widget " + widgetId);
		return;
	}
	var valueWhenOn = jQuery('#' + widgetId).val();
	DWRUtil.setValue(widgetId, value == true || value == valueWhenOn);
}
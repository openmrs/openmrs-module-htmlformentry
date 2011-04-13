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

/**
 * Set the value an HTML element (by name) to the specified value.
 * Based on DWR util setvalue
 * @see http://getahead.org/dwr/browser/util/setvalue
 */
function setValueByName(ele, val, options) {

  if (val == null) val = "";
  if (options == null) options = {};

  var orig = ele;
  
  var nodes = document.getElementsByName(orig);
  if (nodes.length >= 1) ele = nodes.item(0);

  if (ele == null) {
	window.alert("setValueByName() can't find an element with name: " + orig + ".");
    return;
  }

  // All paths now lead to some update so we highlight a change
  dwr.util.highlight(ele, options);

  if (dwr.util._isHTMLElement(ele, "select")) {
    if (ele.type == "select-multiple" && dwr.util._isArray(val)) dwr.util._selectListItems(ele, val);
    else dwr.util._selectListItem(ele, val);
    return;
  }

  if (dwr.util._isHTMLElement(ele, "input")) {
    if (ele.type == "radio" || ele.type == "checkbox") {
      if (nodes && nodes.length >= 1) {
        for (var i = 0; i < nodes.length; i++) {
          var node = nodes.item(i);
          if (node.type != ele.type) continue;
          if (dwr.util._isArray(val)) {
            node.checked = false;
            for (var j = 0; j < val.length; j++)
              if (val[j] == node.value) node.checked = true;
          }
          else {
            node.checked = (node.value == val);
          }
        }
      }
      else {
        ele.checked = (val == true);
      }
    }
    else ele.value = val;

    return;
  }

  if (dwr.util._isHTMLElement(ele, "textarea")) {
    ele.value = val;
    return;
  }

  // If the value to be set is a DOM object then we try importing the node
  // rather than serializing it out
  if (val.nodeType) {
    if (val.nodeType == 9 /*Node.DOCUMENT_NODE*/) val = val.documentElement;
    val = dwr.util._importNode(ele.ownerDocument, val, true);
    ele.appendChild(val);
    return;
  }

  // Fall back to innerHTML and friends
  if (dwr.util._shouldEscapeHtml(options) && typeof(val) == "string") {
    if (ele.textContent) ele.textContent = val;
    else if (ele.innerText) ele.innerText = val;
    else ele.innerHTML = dwr.util.escapeHtml(val);
  }
  else {
    ele.innerHTML = val;
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
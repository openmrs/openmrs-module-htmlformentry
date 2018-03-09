/*
 * Note to HTML Form Entry developers: going forwards new functionality should be put in htmlForm.js
 */

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

function checkNumber(el, errorDivId, floatOkay, absoluteMin, absoluteMax, errorMessages) {
	clearError(errorDivId);

	if (el.value == '') {
		el.className = null;
	}

	var errorMessage = verifyNumber(el, floatOkay, absoluteMin, absoluteMax, errorMessages);
	if (errorMessage == null) {
		el.className = 'legalValue';
		clearError(errorDivId);
	} else {
		el.className = 'illegalValue';
		showError(errorDivId, errorMessage);
	}
}

/**
 * Verifies the numerical value of an input
 * @param el the input element
 * @param floatOkay whether floating point values are acceptable
 * @param absoluteMin the minimum acceptable value (may be null)
 * @param absoluteMax the maximum acceptable value (may be null)
 * @return null or error message
 */
function verifyNumber(el, floatOkay, absoluteMin, absoluteMax, errorMessages) {

	if (!errorMessages) {
		errorMessages = {
			notANumber: jq.get(getContextPath() + "/module/htmlformentry/localizedMessage.form",
                {messageCode: "htmlformentry.error.notANumber"},
                function(responseText) {
                    return responseText;
                }
            ),
			notAnInteger: jq.get(getContextPath() + "/module/htmlformentry/localizedMessage.form",
                {messageCode: "htmlformentry.error.notAnInteger"},
                function(responseText) {
                    return responseText;
                }
            ),
			notLessThan: jq.get(getContextPath() + "/module/htmlformentry/localizedMessage.form",
                {messageCode: "htmlformentry.error.notLessThan"},
                function(responseText) {
                    return responseText;
                }
            ),
			notGreaterThan: jq.get(getContextPath() + "/module/htmlformentry/localizedMessage.form",
                {messageCode: "htmlformentry.error.notGreaterThan"},
                function(responseText) {
                    return responseText;
                }
            )
		}
	}

	var val = el.value.trim();
	if (val == '')
		return null;

	if (floatOkay) {
		if (! /^[+-]?\d*(.\d+)?$/.test(val)) {
			return errorMessages.notANumber;
		}
		val = parseFloat(val);
	} else {
		if (! /^[+-]?\d+$/.test(val)) {
			return errorMessages.notAnInteger;
		}
		val = parseInt(val);
	}

	if (absoluteMin != null) {
		if (val < absoluteMin)
			return errorMessages.notLessThan + " " + absoluteMin;
	}
	if (absoluteMax != null) {
		if (val > absoluteMax)
			return errorMessages.notGreaterThan + " " + absoluteMax;
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

/**
 * Gets the context path of OpenMRS
 */
function getContextPath() {
	var contextPath = typeof OPENMRS_CONTEXT_PATH == 'undefined' ? openmrsContextPath : OPENMRS_CONTEXT_PATH;
	if (contextPath.charAt(0) != '/') {
		contextPath = '/' + contextPath;
	}
	return contextPath;
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

			    jQuery.getJSON(location.protocol + '//' + location.host + getContextPath() + '/module/htmlformentry/' + src
                    + '?answerids=' + answerids + '&answerclasses=' + answerclasses, req, function(data) {

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
				if(select) {//user has selected item from the list
					textField.css('color', 'green');
					textField.trigger("change");

            }
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

	if (hiddenField.val() == "" || hiddenField.val() == "ERROR") {
		if(textField.val() != ""){
			hiddenField.val("ERROR");
			textField.css('color', 'red');
		} else if (textField.val() == ""){
			hiddenField.val("");
		}
	}
}

    /**
     * This is used to provide auto complete when pre populated list of options is given
     * @param element          - autocomplete widget id
     * @param optionnames   - names of the options
     * @param optionvalues    - ids of the options
     */
function setupOptionAutocomplete(element, optionnames, optionvalues){

    var hiddenField = jQuery("#"+element.id+"_hid");
	var textField = jQuery(element);
    var select = false;

    var opnames = optionnames.split(",");
    var opvalues = optionvalues.split(",");
    var optionnamevaluemap = new Object();

    for(var i= 0; i< opnames.length ; i++){
        optionnamevaluemap[opnames[i]] = opvalues[i];
    }

    textField.autocomplete({
       source:opnames,
       minLength:2,
       select:function(event, ui) {
           hiddenField.val(optionnamevaluemap[ui.item.value]);
           select = true;
       },
       close: function(event, ui) {
		    if(select) {//user has selected item from the list
				textField.css('color', 'green');
				textField.trigger("change");
            }
			else {
                textField.css('color', 'red');
				hiddenField.val("ERROR");
			}
			select = false;
	   }
    });
}

/**
 * This is used to handle the deletion/blankout of autocomplete text field. When user deletes existing
   value from text field, this sets the submit value to "".
 * @param element
 */
function setValWhenAutocompleteFieldBlanked(element){

    var textField = jQuery(element);
    var hiddenField = jQuery("#"+element.id+"_hid");

    if(textField.val() === ""){
        hiddenField.val("");
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
    else {
    	setDatePickerValue('#' + orig + '-display', val); // hack for datepicker's display
    	ele.value = val;
    }

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

// fetches the display field for the date instead of the hidden field, so that a change event can be successfully bound to it
function dateFieldGetterFunction(widgetId) {
	return jQuery('#' + widgetId + '-display');
}

// custom setter for date widgets
function dateSetterFunction(widgetId, value) {
    setDatePickerValue('#' + widgetId + '-display', value);
}

//fetches the displayed value within the "read-only text box of new relationship "
function newRelationshipFieldGetterFunction(widgetId) {
	return document.getElementById(widgetId+'name').value;
}

// custom setter for Autocomplete widget
function autocompleteSetterFunction(widgetId, value) {
	if (widgetId == null || document.getElementById(widgetId) == null) {
		window.alert("Form scripting error: cannot find widget " + widgetId);
		return;
	}

	DWRUtil.setValue(widgetId + '_hid', value);
}

// custom getter for Autocomplete widget
function autocompleteGetterFunction(widgetId) {
    return jQuery('#' + widgetId + '_hid').val();
}

//does an ajax lookup to see if this form has already been filled out for this encounter date.
//valid instructions are 'block', and 'warn'
//TODO:  would be nice if this used javascript confirm(), and 'cancel' meant history.go(-1) and/or closed the pop-up window (closing the popup being a bit harder)
function existingEncounterOnDate(item, instruction){

	    if (!(instruction == 'block' || instruction == 'warn'))
	    	return;

		var date = jq(item).val();
		var formId = jq('[name=htmlFormId]').val();
		var patientId = jq('[name=personId]').val();

		if (jq('[name=encounterId]').val() == null)
		{
			jq.get(
                getContextPath() + '/module/htmlformentry/lastEnteredForm.form',
	            {formId: formId, patientId: patientId, date: date, dateFormat: 'yyyy-MM-dd'},
	            function(responseText){

	                if(responseText == "true") {
	                	if (instruction == "warn") {

                			// get the localized warning message and display it
	                		jq.get(getContextPath() + "/module/htmlformentry/localizedMessage.form",
	                				{messageCode: "htmlformentry.error.warnMultipleEncounterOnDate"},
	                				function(responseText) {
                                        jq().toastmessage('showWarningToast',responseText);
	                				}
	                		);

	                	} else if (instruction == "block") {

	                		// get the localized blocking message and display it
	                		jq.get(getContextPath() + "/module/htmlformentry/localizedMessage.form",
	                				{messageCode: "htmlformentry.error.blockMultipleEncounterOnDate"},
	                				function(responseText) {
                                        jq().toastmessage('showErrorToast',responseText);
	                				}
	                		);

		                	//clear the date and continue entering the form
		                	jq(item).val('');
	                	}
	                } else {
	                	//make sure everything is enabled
	                }
	            }
	        );
	    }
}

function setupDatePicker(jsDateFormat,yearsrange, jsLocale, displaySelector, valueSelector, initialDateYMD) {
	if (jsLocale && jsLocale != 'en' && jsLocale != 'en-US') {
		if (!jQuery.datepicker.regional[jsLocale])
			setupDatePickerLocalization(jsLocale);
	}

	var range = yearsrange.split(",");

	var jq = jQuery(displaySelector)
	jq.datepicker({
		dateFormat: jsDateFormat,
		altField: valueSelector,
		altFormat: 'yy-mm-dd',
		gotoCurrent: true,
		changeMonth: true,
		changeYear: true,
		showOtherMonths: true,
		selectOtherMonths: true,
        yearRange: '-'+range[0]+':+'+range[1],
		onSelect: function () {
            jQuery(valueSelector).change();

        }
	});
	if (jsLocale && jQuery.datepicker.regional[jsLocale])
		jq.datepicker('option', jQuery.datepicker.regional[jsLocale]);
	if (initialDateYMD)
		setDatePickerValue(displaySelector, initialDateYMD);

	// register a handler to set the date value to zero if the display widget is emptied (workaround for jquery bug http://bugs.jqueryui.com/ticket/5734)
	jq.change(function () {
		if (jq.val() == null || jq.val() == '') {
			setDatePickerValue(displaySelector, null);
		}
	});
}

function setDatePickerValue(displaySelector, ymd) {
	try {
		if (ymd != null) {
			jQuery(displaySelector).datepicker('setDate', jQuery.datepicker.parseDate('yy-mm-dd', ymd));
		}
		else {
			jQuery(displaySelector).datepicker('setDate', null);
		}
	} catch (err) {
		// make this safe to call with misformatted dates
	}
}

function setupDatePickerLocalization(locale) {
	if (locale == 'es') {
		jQuery.datepicker.regional['es'] = {
			closeText: 'Cerrar',
			prevText: '&#x3c;Ant',
			nextText: 'Sig&#x3e;',
			currentText: 'Hoy',
			monthNames: ['Enero','Febrero','Marzo','Abril','Mayo','Junio',
			    'Julio','Agosto','Septiembre','Octubre','Noviembre','Diciembre'],
			monthNamesShort: ['Ene','Feb','Mar','Abr','May','Jun',
			    'Jul','Ago','Sep','Oct','Nov','Dic'],
			dayNames: ['Domingo','Lunes','Martes','Miércoles','Jueves','Viernes','Sábado'],
			dayNamesShort: ['Dom','Lun','Mar','Mié','Juv','Vie','Sáb'],
			dayNamesMin: ['Do','Lu','Ma','Mi','Ju','Vi','Sá'],
			weekHeader: 'Sm',
			//dateFormat: 'dd/mm/yy',
			isRTL: false,
			showMonthAfterYear: false,
			yearSuffix: ''};
	} else if (locale == 'en-GB') {
		jQuery.datepicker.regional['en-GB'] = {
				closeText: 'Done',
				prevText: 'Prev',
				nextText: 'Next',
				currentText: 'Today',
				monthNames: ['January','February','March','April','May','June',
				    'July','August','September','October','November','December'],
				monthNamesShort: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
				    'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'],
				dayNames: ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'],
				dayNamesShort: ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'],
				dayNamesMin: ['Su','Mo','Tu','We','Th','Fr','Sa'],
				weekHeader: 'Wk',
				//dateFormat: 'dd/mm/yy',
				isRTL: false,
				showMonthAfterYear: false,
				yearSuffix: ''};
	} else if (locale == 'fr') {
		jQuery.datepicker.regional['fr'] = {
			closeText: 'Fermer',
			prevText: '&#x3c;Préc',
			nextText: 'Suiv&#x3e;',
			currentText: 'Courant',
			monthNames: ['Janvier','Février','Mars','Avril','Mai','Juin','Juillet','Août','Septembre','Octobre','Novembre','Décembre'],
			monthNamesShort: ['Jan','Fév','Mar','Avr','Mai','Jun','Jul','Aoû','Sep','Oct','Nov','Déc'],
			dayNames: ['Dimanche','Lundi','Mardi','Mercredi','Jeudi','Vendredi','Samedi'],
			dayNamesShort: ['Dim','Lun','Mar','Mer','Jeu','Ven','Sam'],
			dayNamesMin: ['Di','Lu','Ma','Me','Je','Ve','Sa'],
			weekHeader: 'Sm',
			//dateFormat: 'dd/mm/yy',
			firstDay: 1,
			isRTL: false,
			showMonthAfterYear: false,
			yearSuffix: ''};
	} else if (locale == 'it') {
		jQuery.datepicker.regional['it'] = {
			closeText: 'Chiudi',
			prevText: '&#x3c;Prec',
			nextText: 'Succ&#x3e;',
			currentText: 'Oggi',
			monthNames: ['Gennaio','Febbraio','Marzo','Aprile','Maggio','Giugno',
				'Luglio','Agosto','Settembre','Ottobre','Novembre','Dicembre'],
			monthNamesShort: ['Gen','Feb','Mar','Apr','Mag','Giu',
				'Lug','Ago','Set','Ott','Nov','Dic'],
			dayNames: ['Domenica','Lunedì','Martedì','Mercoledì','Giovedì','Venerdì','Sabato'],
			dayNamesShort: ['Dom','Lun','Mar','Mer','Gio','Ven','Sab'],
			dayNamesMin: ['Do','Lu','Ma','Me','Gi','Ve','Sa'],
			weekHeader: 'Sm',
			//dateFormat: 'dd/mm/yy',
			isRTL: false,
			showMonthAfterYear: false,
			yearSuffix: ''};
	} else if (locale == 'pt') {
		jQuery.datepicker.regional['pt'] = {
			closeText: 'Fechar',
			prevText: '&#x3c;Anterior',
			nextText: 'Pr&oacute;ximo&#x3e;',
			currentText: 'Hoje',
			monthNames: ['Janeiro','Fevereiro','Março','Abril','Maio','Junho',
			    'Julho','Agosto','Setembro','Outubro','Novembro','Dezembro'],
			monthNamesShort: ['Jan','Fev','Mar','Abr','Mai','Jun',
			    'Jul','Ago','Set','Out','Nov','Dez'],
			dayNames: ['Domingo','Segunda-feira','Terça-feira','Quarta-feira','Quinta-feira','Sexta-feira','Sabado'],
			dayNamesShort: ['Dom','Seg','Ter','Qua','Qui','Sex','Sab'],
			dayNamesMin: ['Dom','Seg','Ter','Qua','Qui','Sex','Sab'],
			weekHeader: 'Sm',
			//dateFormat: 'dd/mm/yy',
			isRTL: false,
			showMonthAfterYear: false,
			yearSuffix: ''};
	}
}

openmrs = document.openmrs || {};
openmrs.htmlformentry = openmrs.htmlformentry || {};
//used for dynamicAutocomplete widget
openmrs.htmlformentry.refresh = function(v) {
	var flag = true;
	var string = ((v).split("span", 1)) + "_hid";
	var divId = ((v).split("span", 1)) + "_div";
	var temp = ((v).split("span", 1)) + "span_";
	jq('#' + divId + ' span').each(
			function(index) {
				jq('#' + divId).data("count", index + 1);
				jq('#' + string).attr('value', jq('#' + divId).data("count"));
				flag = false;
				var spanId = this.id;
				var newSpanId = spanId.split('_', 1) + '_' + index;
				this.id = newSpanId;
				jq('#' + spanId + '_hid').attr('id', newSpanId + '_hid');
				jq('#' + spanId + '_button').removeAttr('onclick', null)
						.unbind('click').attr('id', newSpanId + '_button')
						.click(
								function() {
									jq('#' + newSpanId).remove();
									openmrs.htmlformentry.refresh(newSpanId
											+ "_button");
								});
				jq('#' + newSpanId + '_hid').attr('name', newSpanId + '_hid');
			});
	if (flag)
		jq('#' + divId).data("count", 0);
	jq('#' + string).attr('value', jq('#' + divId).data("count"));
}

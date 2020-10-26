//Uses the namespace pattern from http://stackoverflow.com/a/5947280
(function( htmlForm, $, undefined) {

    var onObsChangedCheck = function() {
        var whenValueThenDisplaySection = $(this).data('whenValueThenDisplaySection');

        var val = $(this).val();

        // handle differently autocomplete fields since the obs value is located on the hidden element
        if ($(this).hasClass("ui-autocomplete-input")) {
          val = $("#"+$(this).attr("id")+"_hid").val();
        }

        // handle differently radio sets since the obs value is associated with the name, not id
        // (in radio sets, each radio button has an id in format "w123_x" but all have the same name in format "w123")
        if ($(this).attr('id') && $(this).attr('id').indexOf("_") !== -1) {
          var widgetId = $(this).attr('id').split("_")[0];
          val = $("[name='" + widgetId + "']:checked").val();
        }

        if (whenValueThenDisplaySection) {
            $.each(whenValueThenDisplaySection, function(ifValue, thenSection) {
                if (val == ifValue) {
                    $(thenSection).show();
                } else {
                    $(thenSection).hide();
                    $(thenSection).find('input[type="hidden"], input:text, input:password, input:file, select, textarea').val('');
                    $(thenSection).find('input:checkbox, input:radio').removeAttr('checked').removeAttr('selected');
                }
            });
        }
        var whenValueThenJs = $(this).data('whenValueThenJs');
        if (whenValueThenJs) {
            $.each(whenValueThenJs, function(ifValue, thenJs) {
                if (val == ifValue) {
                    eval(thenJs);
                }
            });
        }
        var whenValueElseJs = $(this).data('whenValueElseJs');
        if (whenValueElseJs) {
            $.each(whenValueElseJs, function(ifValue, elseJs) {
                if (val != ifValue) {
                    eval(elseJs);
                }
            });
        }
    };

    // clears all inputs within the specified contain
    var  clearContainerInputs = function (container) {
        container.find('input[type="hidden"], input:text, input:password, input:file, select, textarea').val('');
        container.find('input:checkbox, input:radio').removeAttr('checked').removeAttr('selected');
    }

    htmlForm.setupWhenThen = function(obsId, valueToSection, valueToThenJs, valueToElseJs) {
        var field = getField(obsId + '.value');
        field.data('whenValueThenDisplaySection', valueToSection);
        field.data('whenValueThenJs', valueToThenJs);
        field.data('whenValueElseJs', valueToElseJs);
        field.change(onObsChangedCheck).change();
    };

    htmlForm.setupObsToggleHandlers = function() {

        // triggered whenever any input with toggleDim attribute is changed.  Currently, only supports
        // checkbox style inputs.
        $('input[toggleDim]').change(function () {
            var target = $(this).attr("toggleDim");
            if ($(this).is(":checked")) {
                $("#" + target + " :input, ." + target + " :input").removeAttr('disabled');
                $("#" + target + ", ." + target).animate({opacity:1.0}, 0);
            } else {
                $("#" + target + " :input, ." + target + " :input").attr('disabled', true);
                $("#" + target + ", ." + target).animate({opacity:0.5}, 100);
                clearContainerInputs($("#" + target + ", ." + target));
            }
        })
            .change()  // immediately trigger a change to initialize

        // triggered whenever any input with toggleHide attribute is changed.  Currently, only supports
        // checkbox style inputs.
        $('input[toggleHide]').change(function () {
            var target = $(this).attr("toggleHide");
            if ($(this).is(":checked")) {
                $("#" + target + ", ." + target).fadeIn();
            } else {
                $("#" + target + ", ." + target).hide();
                clearContainerInputs($("#" + target + ", ." + target));
            }
        })
            .change()  // immediately trigger a change to initialize

    };

    htmlForm.compileMustacheTemplate = function(source) {
        return Handlebars.compile(source);
    };

    htmlForm.preventAutofill = function() {
        $('input').attr("autocomplete", "new-password")
    };

    /*
        Default function called by the <drugOrders> tag tag to initialize and render the contents in all modes
        Primary purpose is to ensure each configured drug section is re-rendered on load and on encounter date change
     */
    htmlForm.initializeDrugOrdersWidgets = function(config) {
        var $encDateHidden = $('#encounterDate').find('input[type="hidden"]');
        var encDate = $encDateHidden.val();

        var $widgetField = $('#' + config.fieldName);

        // Hide all drug sections by default
        $widgetField.find('.drugorders-drug-section').hide();

        // If config format is select, render a drop down widget of drugs to display appropriate one when selected
        if (config.format && config.format === 'select') {
            var $drugSelector = $('<select class="drugorders-drug-selector"></select>');
            var label = (config.selectLabel ? config.selectLabel : 'Choose Drug...');
            $drugSelector.append('<option value="">' + label + '</option>');
            config.drugs.forEach(function(drug) {
               $drugSelector.append('<option value="' + drug.sectionId + '">' + drug.drugLabel + '</option>');
            });
            $drugSelector.change(function() {
                var drugSection = $(this).val();
                if (drugSection !== '') {
                    $('#' + drugSection).show();
                    $(this).val('');
                }
            });
            $widgetField.find('.drugorders-header-section').append($drugSelector);
        }
        // Otherwise, just render all drug sections
        else {
            $widgetField.find('.drugorders-drug-section').show();
        }

        config.drugs.forEach(function(drug) {
            htmlForm.configureDrugOrderWidget(config, null, encDate, drug);
            $encDateHidden.change(function() {
                htmlForm.configureDrugOrderWidget(config, null, $(this).val(), drug);
            });
        });
        console.log(config);
    }

    /**
     * The purpose of this function is to re-initialize a given drug order section, in response to the mode
     * and to interactions on the form (encounter date changes, action changes, etc).
     */
    htmlForm.configureDrugOrderWidget = function(config, action, encDate, drugConfig) {
        var $drugSection = $('#'+drugConfig.sectionId);
        encDate = (encDate === "" ? config.today : encDate);

        // Render drug name details
        var $drugDetailsSection = $drugSection.find(".drugorders-drug-details");
        $drugDetailsSection.html(drugConfig.drugLabel);

        var $historySection = $drugSection.find(".drugorders-order-history");
        $historySection.empty(); // Remove all of the elements

        // If there are existing orders in the encounter, start out by rendering those
        // Also render any order from a previous encounter that was revised by an order in the current encounter
        var lastRenderedOrder = null;
        var lastOrderInEncounter = null;

        var drugHistory = drugConfig.history ? drugConfig.history : new Array();

        drugHistory.forEach(function(drugOrder) {
            if (htmlForm.isOrderInCurrentEncounter(drugOrder, config)) {
                if (drugOrder.previousOrderId !== '') {
                    var prevOrder = htmlForm.getOrder(drugOrder.previousOrderId, drugConfig.history);
                    if (!htmlForm.isOrderInCurrentEncounter(prevOrder, config)) {
                        var $prevOrderElement = htmlForm.formatDrugOrder(prevOrder, encDate, config);
                        $historySection.append($prevOrderElement);
                    }
                }
                var $orderElement = htmlForm.formatDrugOrder(drugOrder, encDate, config);
                $historySection.append($orderElement);
                lastRenderedOrder = drugOrder;
                lastOrderInEncounter = drugOrder;
            }
        });

        // If none were rendered, and mode is view, indicate no orders
        // If none were rendered, and mode is not view, render the active order that is available for revision
        if (lastRenderedOrder == null) {
            if (config.mode === 'VIEW') {
                $historySection.append('<div class="order-view-section drugorders-order-history-none">' + config.translations.noOrders + '</div>');
            }
            else {
                // If there are any active orders on this date, render them
                var activeOrder = null;
                drugHistory.forEach(function(drugOrder) {
                    if (htmlForm.isOrderActive(drugOrder, encDate)) {
                        activeOrder = drugOrder;
                    }
                });
                if (activeOrder != null) {
                    var $lastActiveElement = htmlForm.formatDrugOrder(activeOrder, encDate, config);
                    $historySection.append($lastActiveElement);
                    lastRenderedOrder = activeOrder;
                }
            }
        }

        if (config.mode !== 'VIEW') {
            var $editWidgetSection = $('#' + config.fieldName + '_' + drugConfig.drugId + '_entry');
            // Show edit section, hide all widgets
            $editWidgetSection.find('.order-field-label').hide();
            $editWidgetSection.find('.order-field').hide();
            $editWidgetSection.show();

            // Set up allowed actions
            // Initially, we support a limited set of actions due to complexity of retrospective data entry of orders

            var $actionSection = $editWidgetSection.find('.order-field.action');
            var $actionWidget = $actionSection.find('select');
            $actionWidget.find('option').hide();

            var d = lastRenderedOrder;
            var lastStart = (lastRenderedOrder != null ? lastRenderedOrder.effectiveStartDate.value : '');
            var lastStop = (lastRenderedOrder != null ? lastRenderedOrder.effectiveStopDate.value : '');

            var allowedActions = new Array();
            allowedActions.push("");

            // Allow drugs to be ordered NEW if there are no orders active on or after the encounter date
            if (lastStart === '' || (lastStop !== '' && lastStop <= encDate)) {
                allowedActions.push("NEW");
            }

            // Only RENEW, REVISE, or DISCONTINUE are possible with existing orders
            if (lastStart !== '') {
                // Don't allow any further revisions to a DISCONTINUE order
                if (lastRenderedOrder.action.value !== 'DISCONTINUE') {
                    // Allow REVISION operations if operating on an order with the same or an earlier start date
                    if (lastStart <= encDate) {
                        allowedActions.push("REVISE");
                        allowedActions.push("RENEW");
                        allowedActions.push("DISCONTINUE");
                    }
                }
            }

            allowedActions.forEach(function(action) {
                var $optionElement = $actionWidget.find('option[value="' + action + '"]');
                $optionElement.attr('selected', false);
                $optionElement.show();
            });

            if ($actionWidget.find('option').length > 0) {
                $actionWidget.change(function() {
                    var action = this.value;
                    $editWidgetSection.find('.order-field').hide();
                    $editWidgetSection.find('.order-field.action').show();
                    if (action !== '') {
                        htmlForm.enableDateWidgets($editWidgetSection, encDate);
                    }
                    if (action === 'DISCONTINUE') {
                        $editWidgetSection.find('.discontinueReason').show();
                    }
                    else if (action === 'RENEW') {
                        htmlForm.enableDrugOrderDurationWidgets($editWidgetSection);
                    }
                    else if (action === 'REVISE' || action === 'NEW') {
                        htmlForm.enableDrugOrderDoseWidgets($editWidgetSection);
                        $editWidgetSection.find('.urgency').show();
                        htmlForm.enableDrugOrderDurationWidgets($editWidgetSection);
                    }
                });
                $actionSection.show();
                $actionSection.children().show();
            }

            // Set up ability to toggle between free-text and simple dosing instructions

            $editWidgetSection.find('.dosingType').find('input:radio').change(function() {
                htmlForm.enableDrugOrderDoseWidgets($editWidgetSection);
            });

            // Set up ability to toggle between scheduled and non-scheduled urgencies
            $editWidgetSection.find('.urgency').find('input:radio').change(function() {
                htmlForm.enableDateWidgets($editWidgetSection, encDate);
            });
        }
    }

    htmlForm.enableDateWidgets = function($editWidgetSection, encDate) {

        // Do not allow editing date activated, and always inherit encounter date.
        $editWidgetSection.find('.dateActivated').show();
        var $dateActivatedSection = $editWidgetSection.find('.order-field.dateActivated');
        var $dateActivatedWidget = $dateActivatedSection.find('.order-field-widget.dateActivated');
        var $dateActivatedTextField = $dateActivatedWidget.find('input[type=text]');
        setDatePickerValue($dateActivatedTextField, (encDate));
        $dateActivatedWidget.hide();
        $dateActivatedSection.find('.value').remove();
        $dateActivatedSection.append('<span class="value">' + $dateActivatedTextField.val() + '</span>')
        $dateActivatedSection.show();

        // Allow scheduled date to be set
        var $urgencySection = $editWidgetSection.find('.urgency');
        var urgencyVal = $urgencySection.find('input:checked').val();
        if (urgencyVal === 'ON_SCHEDULED_DATE') {
            $editWidgetSection.find('.scheduledDate').show();
        }
        else {
            $editWidgetSection.find('.scheduledDate').hide();
        }
    };

    htmlForm.enableDrugOrderDoseWidgets = function($editWidgetSection) {
        var $dosingTypeSection = $editWidgetSection.find('.dosingType');
        var dosingTypeVal = $dosingTypeSection.find('input:checked').val();
        if (dosingTypeVal === 'org.openmrs.FreeTextDosingInstructions') {
            $editWidgetSection.find('.dose').hide();
            $editWidgetSection.find('.doseUnits').hide();
            $editWidgetSection.find('.frequency').hide();
            $editWidgetSection.find('.route').hide();
            $editWidgetSection.find('.asNeeded').hide();
            $editWidgetSection.find('.instructions').hide();
            $editWidgetSection.find('.dosingInstructions').show();
        }
        else {
            $editWidgetSection.find('.dose').show();
            $editWidgetSection.find('.doseUnits').show();
            $editWidgetSection.find('.frequency').show();
            $editWidgetSection.find('.route').show();
            $editWidgetSection.find('.asNeeded').show();
            $editWidgetSection.find('.instructions').show();
            $editWidgetSection.find('.dosingInstructions').hide();
        }
        $dosingTypeSection.show();
    }

    htmlForm.enableDrugOrderDurationWidgets = function($editWidgetSection) {
        $editWidgetSection.find('.duration').show();
        $editWidgetSection.find('.durationUnits').show();
        $editWidgetSection.find('.quantity').show();
        $editWidgetSection.find('.quantityUnits').show();
        $editWidgetSection.find('.numRefills').show();
    }

    htmlForm.formatDrugOrder = function(d, encDate, config) {
        var $ret = $('<div class="drugorders-order-history-item"></div>');

        var $existingActionSection = $('<div class="order-view-section order-view-action"></div>');
        var inCurrentEncounter = htmlForm.isOrderInCurrentEncounter(d, config);
        if (!inCurrentEncounter) {
            $ret.addClass('order-view-different-encounter');
            $existingActionSection.append(config.translations.previousOrder);
        }
        else {
            $ret.addClass('order-view-current-encounter');
            $ret.addClass('value');
            $existingActionSection.append(d.action.display);
        }
        var isActive = htmlForm.isOrderActive(d, encDate);
        $ret.addClass(isActive ? "order-view-active" : "order-view-inactive")
        $ret.append($existingActionSection);

        var $dateSection = $('<div class="order-view-section order-view-dates"></div>');
        $dateSection.append('<div class="order-view-field order-view-start-date">' + d.effectiveStartDate.display + '</div>');
        var endDate = (d.effectiveStopDate.display === "" ? config.translations.present : d.effectiveStopDate.display);
        $dateSection.append(' - <div class="order-view-field order-view-stop-date">' + endDate + '</div>');
        $ret.append($dateSection);

        var $doseSection = $('<div class="order-view-section order-view-dosing"></div>');
        if (d.dosingType.value === 'org.openmrs.FreeTextDosingInstructions') {
            $doseSection.append('<div class="order-view-field order-view-dosing-instructions">' + d.dosingInstructions.display + "</div>");
        } else {
            if (d.dose.display !== '') {
                $doseSection.append('<div class="order-view-field order-view-dose">' + d.dose.display + " " + d.doseUnits.display + "</div>");
            }
            if (d.route.display !== "") {
                $doseSection.append(' -- <div class="order-view-field order-view-route">' + d.route.display + '</div>');
            }
            if (d.frequency.display !== "") {
                $doseSection.append(' -- <div class="order-view-field order-view-frequency">' + d.frequency.display + '</div>');
            }
            if (d.asNeeded.value === "true") {
                $doseSection.append(' -- <div class="order-view-field order-view-as-needed">' + config.translations.asNeeded + '</div>');
            }
            if (d.instructions.value !== "") {
                $doseSection.append(' -- <div class="order-view-field order-view-instructions">' + d.instructions.display + '</div>');
            }
        }
        $ret.append($doseSection);
        return $ret;
    }

    htmlForm.getOrder = function(orderId, history) {
        var ret = null;
        history.forEach(function(drugOrder) {
            if (drugOrder.orderId === orderId) {
                ret =  drugOrder;
            }
        });
        return ret;
    }

    htmlForm.isOrderInCurrentEncounter = function(order, config) {
        return order.encounterId === config.encounterId
    }

    /**
     * Helper method which returns true if the given order is active on the given date
     */
    htmlForm.isOrderActive = function(drugOrder, onDate) {
        if (drugOrder.dateActivated.value > onDate) {
            return false;
        }
        if (drugOrder.effectiveStopDate.value !== '' && drugOrder.effectiveStopDate.value <= onDate) {
            return false;
        }
        return true;
    }

    // any users of this library should call this function during page load to make sure that all elements are properly initialized
    // if new functionality is added that requires setup, the setup function should be called from here
    htmlForm.initialize = function() {
        htmlForm.setupObsToggleHandlers();
        htmlForm.preventAutofill();
    }

}( window.htmlForm = window.htmlForm || {}, jQuery ));

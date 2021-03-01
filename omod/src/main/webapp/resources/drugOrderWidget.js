//Uses the namespace pattern from http://stackoverflow.com/a/5947280
(function( drugOrderWidget, $, undefined) {

    drugOrderWidget.getEncounterDate = function(defaultDate) {
        var $encDateHidden = $('#encounterDate').find('input[type="hidden"]');
        var encDate = $encDateHidden.val();
        if (!encDate || encDate === '') {
            encDate = defaultDate;
        }
        return encDate;
    }

    drugOrderWidget.getOrder = function(orderId, history) {
        var ret = null;
        history.forEach(function(drugOrder) {
            if (drugOrder.orderId === orderId) {
                ret =  drugOrder;
            }
        });
        return ret;
    }

    drugOrderWidget.getConfigForConcept = function(config, conceptId) {
        var ret = null;
        config.concepts.forEach(function(concept) {
            if (concept.conceptId === conceptId) {
                ret = concept;
            }
        });
        return ret;
    }

    drugOrderWidget.nextDrugSectionIndex = function() {
        return $('.drugorders-drug-section').length;
    }

    drugOrderWidget.nextActionButtonIndex = function() {
        return $('.order-action-button').length;
    }

    drugOrderWidget.isOrderInCurrentEncounter = function(order, config) {
        return order.encounterId === config.encounterId
    }

    /**
     * Helper method which returns true if the given order is active on the given date
     */
    drugOrderWidget.isOrderActive = function(drugOrder, onDate) {
        if (drugOrder.dateActivated.value > onDate) {
            return false;
        }
        if (drugOrder.effectiveStopDate.value !== '' && drugOrder.effectiveStopDate.value <= onDate) {
            return false;
        }
        return true;
    }

    // If the order was placed in this encounter, and it was a revision of a previous order, render the previous order
    drugOrderWidget.shouldRenderPreviousOrder = function(drugOrder, config) {
        return drugOrderWidget.isOrderInCurrentEncounter(drugOrder, config) && (drugOrder.previousOrderId !== '');
    }

    /* If the order was placed in this encounter, or if is active at the time of the encounter, then render it
       We also check if dateStopped is null.  This will result in hiding these if a historical encounter is edited and the order has been subsequently closed.
     */
    drugOrderWidget.shouldRenderDrugOrder = function(drugOrder, config, encDate) {
        return drugOrderWidget.isOrderInCurrentEncounter(drugOrder, config) || (
            config.mode !== 'VIEW' && drugOrderWidget.isOrderActive(drugOrder, encDate) && drugOrder.dateStopped.value === ""
        );
    }

    // If an order has subsequently been revised, do not allow editing
    drugOrderWidget.canEditDrugOrder = function(drugOrder, config, encDate) {
        if (drugOrderWidget.shouldRenderDrugOrder(drugOrder, config, encDate)) {
            config.history.forEach(function (historyOrder) {
                if (historyOrder.previousOrderId === drugOrder.orderId) {
                    return false;
                }
            });
            return true;
        }
        return false;
    }

    // Renew is allowed for active orders that are not placed in the same encounter
    drugOrderWidget.canRenewDrugOrder = function(drugOrder, config) {
        var ret = drugOrderWidget.supportsAction(config, 'RENEW');
        ret = ret && drugOrder.action.value !== 'DISCONTINUE';
        ret = ret && !drugOrderWidget.isOrderInCurrentEncounter(drugOrder, config);
        return ret;
    }

    // Revise is allowed for active orders.  This is treated as a void/new if the encounter is the same on the backend
    drugOrderWidget.canReviseDrugOrder = function(drugOrder, config) {
        var ret = drugOrderWidget.supportsAction(config, 'REVISE');
        ret = ret && drugOrder.action.value !== 'DISCONTINUE';
        return ret;
    }

    // Discontinue is allowed for active orders.
    drugOrderWidget.canDiscontinueDrugOrder = function(drugOrder, config) {
        var supportsAction = drugOrderWidget.supportsAction(config, 'DISCONTINUE');
        var inEncounter = drugOrderWidget.isOrderInCurrentEncounter(drugOrder, config);
        var notDiscontinued = drugOrder.action.value !== 'DISCONTINUE'
        return supportsAction && (inEncounter || notDiscontinued);
    }

    drugOrderWidget.getActionOption = function(config, action) {
        var $orderTemplate = $('#' + config.fieldName + '_template');
        var $actionSection = $orderTemplate.find('.order-field.order-action');
        var $actionWidget = $actionSection.find('select');
        var $optionElement = $actionWidget.find('option[value="' + action + '"]');
        return $optionElement;
    }

    drugOrderWidget.supportsAction = function(config, action) {
        return drugOrderWidget.getActionOption(config, action).length > 0;
    }

    /**
     * Default function called by the <drugOrders> tag tag to initialize and render the contents in all modes
     * Primary purpose is to ensure each configured drug section is re-rendered on load and on encounter date change
     */
    drugOrderWidget.initialize = function(config) {

        console.log(config);

        // Get the section containing the html for this section
        var $widgetField = $('#' + config.fieldName);

        // Initialize all of the drug sections
        drugOrderWidget.renderDrugOrdersForRevision(config);

        // Set up watch for an encounter date change.  If date changes, re-initialize all drug sections
        var $encDateHidden = $('#encounterDate').find('input[type="hidden"]');
        $encDateHidden.change(function() {
            if ($('.drugorders-order-form').length > 1) { // More than 1, since the template is present
                alert(config.translations.encounterDateChangeWarning);
            }
            drugOrderWidget.renderDrugOrdersForRevision(config);
        });

        // Render ordering actions
        if (config.mode !== 'VIEW') {
            var $newOrderSection = drugOrderWidget.createNewOrderSection(config);
            $widgetField.find('.drugorders-selector-section').append($newOrderSection);
        }
    }

    drugOrderWidget.renderDrugOrdersForRevision = function(config) {
        var $widgetField = $('#' + config.fieldName);
        $widgetField.find(".drugorders-order-section").empty();

        var encDate = drugOrderWidget.getEncounterDate(config.defaultDate);

        config.history.forEach(function(drugOrder) {
            if (drugOrderWidget.shouldRenderDrugOrder(drugOrder, config, encDate)) {
                drugOrderWidget.renderDrugOrder(drugOrder, config, encDate);
            }
        });
    }

    drugOrderWidget.addDrugOrderSection = function(config) {
        var $elementSection = $('#' + config.fieldName);
        var $ordersSection = $elementSection.find('.drugorders-order-section');
        var drugSectionId = 'drugorders-drug-section-' + drugOrderWidget.nextDrugSectionIndex();
        var $drugSection = $('<div id="' + drugSectionId + '" class="drugorders-drug-section"></div>');
        $ordersSection.append($drugSection);
        return $drugSection;
    }

    /**
     * Renders a given drug order
     */
    drugOrderWidget.renderDrugOrder = function(drugOrder, config, encDate) {

        var $drugSection = drugOrderWidget.addDrugOrderSection(config);
        var $historySection = $('<div class="drugorders-drug-history"></div>');
        $drugSection.append($historySection);
        $historySection.empty();

        $historySection.append(drugOrderWidget.formatOrderable(drugOrder));

        // If the order was placed in this encounter, and it was a revision of a previous order, render the previous order
        if (drugOrderWidget.shouldRenderPreviousOrder(drugOrder, config)) {
            var prevOrder = drugOrderWidget.getOrder(drugOrder.previousOrderId, config.history);
            // If an order in this encounter was made to revise another encounter, show the encounter that was revised
            if (!drugOrderWidget.isOrderInCurrentEncounter(prevOrder, config)) {
                var $prevOrderElement = drugOrderWidget.formatDrugOrder(prevOrder, encDate, config);
                $historySection.append($prevOrderElement);
            }
        }

        var $orderElement = drugOrderWidget.formatDrugOrder(drugOrder, encDate, config);
        $historySection.append($orderElement);

        var $actionSection = drugOrderWidget.createEditOrderSections(drugOrder, config, encDate);
        $drugSection.append($actionSection);

        // Ensure this section is visible
        $drugSection.show();
        return $drugSection;
    }

    drugOrderWidget.getSupportedActions = function(drugOrder, config, encDate) {
        var ret = [];
        if (drugOrderWidget.canEditDrugOrder(drugOrder, config, encDate)) {
            if (drugOrderWidget.canRenewDrugOrder(drugOrder, config)) {
                ret.push('RENEW');
            }
            if (drugOrderWidget.canReviseDrugOrder(drugOrder, config)) {
                ret.push('REVISE');
            }
            if (drugOrderWidget.canDiscontinueDrugOrder(drugOrder, config)) {
                ret.push('DISCONTINUE');
            }
        }
        return ret;
    }

    /**
     * Clones the order form template and replaces all the the ids and names as appropriate
     */
    drugOrderWidget.constructOrderForm = function($sectionToAppendTo, idSuffix, config, action) {

        // Clone the order form template, ensuring ids and names of widgets are configured for this specific drug
        var $orderForm = $('#' + config.fieldName + '_template').clone();
        $orderForm.find("[id]").add($orderForm).each(function () {
            this.id = this.id + idSuffix;
        });
        $orderForm.find("[name]").add($orderForm).each(function () {
            this.name = this.name + idSuffix;
        });

        // This is necessary to ensure each NumberFieldWidget interacts with the associated, cloned errorWidget
        $orderForm.find('input[onblur*=checkNumber]').attr('onblur', function (index, currentValue) {
            var split = currentValue.split(/(checkNumber\(this,')(w\d*)('*)/);
            var newFn = '';
            split.forEach(function (val) {
                if (RegExp(/w\d*/).test(val)) {
                    val = val + idSuffix;
                }
                newFn += val;
            })
            return newFn;
        });

        $orderForm.hide();
        $sectionToAppendTo.append($orderForm);

        // Hide all widgets by default
        $orderForm.find('.order-field-label').hide();
        $orderForm.find('.order-field').hide();
        $orderForm.show();

        var encDate = drugOrderWidget.getEncounterDate(config.defaultDate);

        // Ensure the action is set to the configured value
        var $actionSection = $orderForm.find('.order-field.order-action');
        var $actionWidget = $actionSection.find('select');
        $actionWidget.val(action);
        $actionWidget.hide();

        // Render the appropriate fields
        drugOrderWidget.enableDateWidgets(config, $orderForm, encDate);

        if (action === 'DISCONTINUE') {
            var $discontinueReasonSelect = $orderForm.find('.order-field-widget.order-discontinueReason').find('select');
            if ($discontinueReasonSelect.find('option').length > 1) {
                $orderForm.find('.order-discontinueReason').show();
            }
            $orderForm.find('.order-discontinueReasonNonCoded').show();
        } else if (action === 'RENEW') {
            drugOrderWidget.enableDrugOrderDurationWidgets($orderForm);
        } else if (action === 'REVISE' || action === 'NEW') {
            $orderForm.find('.order-orderReason').show();
            drugOrderWidget.enableDrugOrderDoseWidgets($orderForm);
            $orderForm.find('.order-urgency').show();
            drugOrderWidget.enableDrugOrderDurationWidgets($orderForm);
        }

        // Set up ability to toggle between free-text and simple dosing instructions
        $orderForm.find('.order-dosingType').find('input:radio').change(function () {
            if (action === 'REVISE' || action === 'NEW') {
                drugOrderWidget.enableDrugOrderDoseWidgets($orderForm);
            }
        });

        // Set up ability to toggle between scheduled and non-scheduled urgencies
        $orderForm.find('.order-urgency').find('input:radio').change(function () {
            drugOrderWidget.enableDateWidgets(config, $orderForm, encDate);
        });

        return $orderForm;
    }

    drugOrderWidget.createActionButton = function(idSuffix, action, label) {
        return $('<div id="order-action-button-' + action + idSuffix + '" class="order-action-button">' + label + '</div>');
    }

    /**
     * The purpose of this function is to construct selectors and form for entering a NEW order
     */
    drugOrderWidget.createNewOrderSection = function(config) {
        var $actionOption = drugOrderWidget.getActionOption(config, 'NEW');
        var idSuffix = '_' + drugOrderWidget.nextActionButtonIndex();
        var $newButton = drugOrderWidget.createActionButton(idSuffix, 'NEW', $actionOption.html());
        $newButton.click(function () {
            var $drugOrderSection = drugOrderWidget.addDrugOrderSection(config);
            var $orderForm = drugOrderWidget.constructOrderForm($drugOrderSection, idSuffix, config, 'NEW');

            var $actionSection = $('<div class="order-action-section"></div>');
            var $deleteAction = $('<div class="order-action-button">' + config.translations.delete + '</div>');
            $deleteAction.click(function(event) {
                $drugOrderSection.remove();
            });
            $actionSection.append($deleteAction);
            $drugOrderSection.append($actionSection);

            drugOrderWidget.enableDrugSelector(config, $orderForm);
            drugOrderWidget.enableContextWidgets(config, $orderForm);
            $orderForm.show();
        });
        return $newButton;
    }

    drugOrderWidget.createEditOrderSections = function(drugOrder, config, encDate) {
        var $orderActionSection = $('<div class="order-action-section"></div>');
        var $orderActionButtons = $('<div class="order-action-buttons"></div>');
        $orderActionSection.append($orderActionButtons);
        var $orderActionWarningSection = $("<div class='order-action-warnings'>" + config.translations.editDeleteWarning + "</div>");
        $orderActionSection.append($orderActionWarningSection);
        $orderActionWarningSection.hide();
        var $orderActionForms = $('<div class="order-action-forms"></div>');
        $orderActionSection.append($orderActionForms);

        var orderActions = drugOrderWidget.getSupportedActions(drugOrder, config, encDate);

        orderActions.forEach(function(action) {
            var $actionOption = drugOrderWidget.getActionOption(config, action);
            var idSuffix = '_' + drugOrderWidget.nextActionButtonIndex();

            var actionLabel = $actionOption.html();
            var isEditingPreviousEncounter = drugOrderWidget.isOrderInCurrentEncounter(drugOrder, config);
            var isRevising = (action === 'REVISE');
            var isDiscontinuing = (action === 'DISCONTINUE');

            if (isEditingPreviousEncounter) {
                if (isRevising) {
                    actionLabel = config.translations.editOrder;
                }
                else if (isDiscontinuing) {
                    actionLabel = config.translations.deleteOrder;
                }
            }

            var $actionButton = drugOrderWidget.createActionButton(idSuffix, action, actionLabel);
            $actionButton.click(function() {
                var $orderForm = $orderActionForms.find('.drugorders-order-form');
                $orderActionWarningSection.hide();
                $orderActionButtons.find(".order-action-button").hide();
                if ($orderForm.length > 0) {
                    $orderForm.remove();
                    $actionButton.removeClass('drugorders-selected-action');
                    $orderActionButtons.find(".order-action-button").show();
                }
                else {
                    $actionButton.addClass('drugorders-selected-action');
                    $actionButton.show();
                    $orderForm = drugOrderWidget.constructOrderForm($orderActionForms, idSuffix, config, action);
                    drugOrderWidget.populateOrderForm(config, $orderForm, drugOrder);
                    if (isEditingPreviousEncounter) {
                        if (isDiscontinuing) {
                            $orderForm.hide();
                        } else {
                            $orderForm.show();
                        }
                        if (isRevising || isDiscontinuing) {
                            $orderActionWarningSection.show();
                        }
                    }
                }
            });
            $orderActionButtons.append($actionButton);
        });

        return $orderActionSection;
    }

    // Enable selecting formulations / drugs for the selected concept
    drugOrderWidget.enableDrugSelector = function(config, $orderForm) {
        var $conceptSelect = $orderForm.find('.order-field-widget.order-concept').find('select');
        var $drugSelect = $orderForm.find('.order-field-widget.order-drug').find('select');
        var $drugElements = $orderForm.find('.order-drug');
        var $drugNonCodedElements = $orderForm.find('.order-drugNonCoded');

        if ($conceptSelect.is(":visible")) {
            if (config.orderPropertyAttributes?.concept?.style === 'autocomplete') {
                drugOrderWidget.convertToAutocomplete($conceptSelect);
            }
            $drugElements.hide();
            $drugNonCodedElements.hide();
            $conceptSelect.change(function () {
                $drugElements.hide();
                $drugNonCodedElements.hide();
                var conceptId = $conceptSelect.val();
                if (conceptId !== '') {
                    $drugSelect.find("option").hide();
                    $drugSelect.find('option[value=""]').show();
                    $drugSelect.val("");
                    var concept = drugOrderWidget.getConfigForConcept(config, conceptId);
                    concept.drugs.forEach(function (drug) {
                        $drugSelect.find('option[value="' + drug.drugId + '"]').show();
                        $drugElements.show();
                    })
                    $drugNonCodedElements.show();
                }
            });
            $orderForm.find('.order-concept').show();
        }
        else {
            $drugElements.show();
            $drugNonCodedElements.hide();
            if (config.orderPropertyAttributes?.drug?.style === 'autocomplete') {
                drugOrderWidget.convertToAutocomplete($drugSelect);
            }
        }
    }

    // If there was no template configured, show or set defaults where necessary
    drugOrderWidget.enableContextWidgets = function(config, $orderForm) {
        if (config.hasTemplate === 'false') {
            $orderForm.find('.order-careSetting').show();

            var $orderTypeSelect = $orderForm.find('.order-field-widget.order-orderReason').find(':input');
            if ($orderTypeSelect.find('option').length > 1) {
                $orderForm.find('.order-orderType').show();
            }
        }
        var $orderReasonSelect = $orderForm.find('.order-field-widget.order-orderReason').find(':input');
        if ($orderReasonSelect.find('option').length > 1) {
            $orderForm.find('.order-orderReason').show();
        }
    }

    drugOrderWidget.enableDateWidgets = function(config, $orderForm, encDate) {

        // Because the form was cloned, date picker widgets need to be re-enabled
        $orderForm.find('.hasDatepicker').each(function() {
            var dispId = '#' + this.id;
            var valId = '#' + this.id.replace('-display', '');
            $(this).removeClass('hasDatepicker').removeData('datepicker').unbind();  // Reset cloned datepicker
            var dateConfig = config.dateWidgetConfig;
            setupDatePicker(dateConfig.dateFormat, dateConfig.yearsRange, dateConfig.locale, dispId, valId, '');
        });

        // Do not allow editing date activated, and always inherit encounter date.
        $orderForm.find('.order-dateActivated').show();
        var $dateActivatedSection = $orderForm.find('.order-field.order-dateActivated');
        var $dateActivatedWidget = $dateActivatedSection.find('.order-field-widget.order-dateActivated');
        var $dateActivatedTextField = $dateActivatedWidget.find('input[type=text]');
        setDatePickerValue($dateActivatedTextField, (encDate));
        $dateActivatedWidget.hide();
        $dateActivatedSection.find('.value').remove();
        $dateActivatedSection.append('<span class="value">' + $dateActivatedTextField.val() + '</span>')
        $dateActivatedSection.show();

        // Allow scheduled date to be set
        var $urgencySection = $orderForm.find('.order-urgency');
        var urgencyVal = $urgencySection.find('input:checked').val();
        if (urgencyVal === 'ON_SCHEDULED_DATE') {
            $orderForm.find('.order-scheduledDate').show();
        }
        else {
            $orderForm.find('.order-scheduledDate').hide();
        }
    };

    drugOrderWidget.enableDrugOrderDoseWidgets = function($orderForm) {
        var $dosingTypeSection = $orderForm.find('.order-dosingType');
        var dosingTypeVal = $dosingTypeSection.find('input:checked').val();
        if (dosingTypeVal === 'org.openmrs.FreeTextDosingInstructions') {
            $orderForm.find('.order-dose').hide();
            $orderForm.find('.order-doseUnits').hide();
            $orderForm.find('.order-frequency').hide();
            $orderForm.find('.order-route').hide();
            $orderForm.find('.order-asNeeded').hide();
            $orderForm.find('.order-instructions').hide();
            $orderForm.find('.order-dosingInstructions').show();
        }
        else {
            $orderForm.find('.order-dose').show();
            $orderForm.find('.order-doseUnits').show();
            $orderForm.find('.order-frequency').show();
            $orderForm.find('.order-route').show();
            $orderForm.find('.order-asNeeded').show();
            $orderForm.find('.order-instructions').show();
            $orderForm.find('.order-dosingInstructions').hide();
        }
        $dosingTypeSection.show();
    }

    drugOrderWidget.enableDrugOrderDurationWidgets = function($orderForm) {
        $orderForm.find('.order-duration').show();
        $orderForm.find('.order-durationUnits').show();
        $orderForm.find('.order-quantity').show();
        $orderForm.find('.order-quantityUnits').show();
        $orderForm.find('.order-numRefills').show();
    }

    drugOrderWidget.populateOrderForm = function(config, $orderForm, drugOrder) {
        $orderForm.find('.order-field-widget.order-previousOrder').find(':input').val(drugOrder.orderId);
        $orderForm.find('.order-field-widget.order-concept').find(':input').val(drugOrder.concept.value);
        $orderForm.find('.order-field-widget.order-drug').find(':input').val(drugOrder.drug.value);
        $orderForm.find('.order-field-widget.order-drugNonCoded').find(':input').val(drugOrder.drugNonCoded.value);
        $orderForm.find('.order-field-widget.order-orderReason').find(':input').val(drugOrder.orderReason.value);
        $orderForm.find('.order-field-widget.order-orderReasonNonCoded').find(':input').val(drugOrder.orderReasonNonCoded.value);
        $orderForm.find('.order-field-widget.order-careSetting').find(':input').val(drugOrder.careSetting.value);
        $orderForm.find('.order-field-widget.order-dosingType').find(':input[value="' + drugOrder.dosingType.value + '"]').click();
        $orderForm.find('.order-field-widget.order-orderType').find(':input').val(drugOrder.orderType.value);
        $orderForm.find('.order-field-widget.order-dosingInstructions').find(':input').val(drugOrder.dosingInstructions.value);
        $orderForm.find('.order-field-widget.order-dose').find(':input').val(drugOrder.dose.value);
        $orderForm.find('.order-field-widget.order-doseUnits').find(':input').val(drugOrder.doseUnits.value);
        $orderForm.find('.order-field-widget.order-route').find(':input').val(drugOrder.route.value);
        $orderForm.find('.order-field-widget.order-frequency').find(':input').val(drugOrder.frequency.value);
        if (drugOrder.asNeeded.value === 'true') {
            $orderForm.find('.order-field-widget.order-asNeeded').find(':input').attr('checked', 'true');
        }
        $orderForm.find('.order-field-widget.order-instructions').find(':input').val(drugOrder.instructions.value);
        $orderForm.find('.order-field-widget.order-duration').find(':input').val(drugOrder.duration.value);
        $orderForm.find('.order-field-widget.order-durationUnits').find(':input').val(drugOrder.durationUnits.value);
        $orderForm.find('.order-field-widget.order-quantity').find(':input').val(drugOrder.quantity.value);
        $orderForm.find('.order-field-widget.order-quantityUnits').find(':input').val(drugOrder.quantityUnits.value);
        $orderForm.find('.order-field-widget.order-numRefills').find(':input').val(drugOrder.numRefills.value);
    }

    drugOrderWidget.formatOrderable = function(drugOrder) {
        // Render drug name details
        var $ret = $('<div class="order-view-section drugorders-drug-details"></div>');
        $ret.append('<div class="drugorders-drug-details-concept">' + drugOrder.concept.display  + '</div>');
        if (drugOrder.drug.value !== '') {
            $ret.append('<div class="drugorders-drug-details-drug">' + drugOrder.drug.display + '</div>');
        }
        if (drugOrder.drugNonCoded.value !== '') {
            $ret.append('<div class="drugorders-drug-details-drugNonCoded">' + drugOrder.drugNonCoded.display + '</div>');
        }
        return $ret;
    }

    drugOrderWidget.formatDrugOrder = function(d, encDate, config) {
        var $ret = $('<div class="drugorders-order-history-item"></div>');

        var isActive = drugOrderWidget.isOrderActive(d, encDate);
        $ret.addClass(isActive ? "order-view-active" : "order-view-inactive")

        var $existingActionSection = $('<div class="order-view-section order-view-field order-view-action"></div>');
        var inCurrentEncounter = drugOrderWidget.isOrderInCurrentEncounter(d, config);
        if (!inCurrentEncounter) {
            $ret.addClass('order-view-different-encounter');
            if (drugOrderWidget.isOrderActive(d, encDate)) {
                $existingActionSection.append(config.translations['active']);
            }
            else {
                $existingActionSection.append(config.translations.previousOrder);
            }
        }
        else {
            $ret.addClass('order-view-current-encounter');
            $ret.addClass('value');
            $existingActionSection.append(d.action.display);
        }
        $ret.append($existingActionSection);

        if (d.orderReason.display !== '' || d.orderReasonNonCoded.display !== '') {
            var $reasonSection = $('<div class="order-view-section order-view-reasons"></div>');
            $reasonSection.append('<div class="order-view-field order-view-orderReason-label">' + config.translations.orderReason + '</div>');
            if (d.orderReason.display !== '') {
                $reasonSection.append('<div class="order-view-field order-view-orderReason">' + d.orderReason.display + '</div>');
            }
            if (d.orderReasonNonCoded.display !== '') {
                $reasonSection.append('<div class="order-view-field order-view-orderReasonNonCoded">' + d.orderReasonNonCoded.display + '</div>');
            }
        }
        $ret.append($reasonSection);

        var isDiscontinue = (d.action.value === 'DISCONTINUE');

        var $dateSection = $('<div class="order-view-section order-view-dates"></div>');
        $dateSection.append('<div class="order-view-field order-view-start-date">' + config.translations.starting + ' ' + d.effectiveStartDate.display + "</div>");
        if (d.action.value !== 'DISCONTINUE') {
            if (d.duration.display !== '') {
                $dateSection.append(' ' + config.translations['for'] + ' ' + d.duration.display + ' ' + d.durationUnits.display);
            } else if (d.autoExpireDate.display !== '') {
                $dateSection.append('<div class="order-view-field order-view-stop-date">');
                $dateSection.append(config.translations.until + ' ' + d.autoExpireDate.display);
                $dateSection.append('</div>');
            }
        }
        $dateSection.append('</div>');
        $ret.append($dateSection);

        if (isDiscontinue) {
            var $discontinueSection = $('<div class="order-view-section order-view-discontinue"></div>');
            $discontinueSection.append('<div class="order-view-field order-view-discontinue-reason-label">' + config.translations.discontinueReason + ': </div>');
            $discontinueSection.append('<div class="order-view-field order-view-discontinue-reason">' + d.discontinueReason.display + d.discontinueReasonNonCoded.display + '</div>');
            $ret.append($discontinueSection);
        }
        else {
            var $doseSection = $('<div class="order-view-section order-view-dosing"></div>');
            if (d.dosingType.value === 'org.openmrs.FreeTextDosingInstructions') {
                $doseSection.append('<div class="order-view-field order-view-dosing-instructions">' + d.dosingInstructions.display + "</div>");
            } else {
                if (d.dose.display !== '') {
                    $doseSection.append('<div class="order-view-field order-view-dose">' + d.dose.display + " " + d.doseUnits.display + "</div>");
                }
                if (d.route.display !== "") {
                    $doseSection.append('<div class="order-view-field order-view-route">' + d.route.display + '</div>');
                }
                if (d.frequency.display !== "") {
                    $doseSection.append('<div class="order-view-field order-view-frequency">' + d.frequency.display + '</div>');
                }
                if (d.asNeeded.value === "true") {
                    $doseSection.append('<div class="order-view-field order-view-as-needed">' + config.translations.asNeeded + '</div>');
                }
                if (d.instructions.value !== "") {
                    $doseSection.append('<div class="order-view-field order-view-instructions">' + d.instructions.display + '</div>');
                }
            }
            $ret.append($doseSection);

            var $quantitySection = $('<div class="order-view-section order-view-quantity-section"></div>');
            if (d.quantity.display !== '') {
                $quantitySection.append('<div class="order-view-field order-view-quantity-label">' + config.translations.quantity + ': </div>');
                $quantitySection.append('<div class="order-view-field order-view-quantity">' + d.quantity.display + '</div>');
                $quantitySection.append('<div class="order-view-field order-view-quantityUnits">' + d.quantityUnits.display + '</div>');
            }
            if (d.numRefills.display !== "") {
                $quantitySection.append('<div class="order-view-field order-view-numRefills">' + d.numRefills.display + '</div>');
                $quantitySection.append('<div class="order-view-field order-view-numRefills-label">' + config.translations.refills + '</div>');
            }
            $ret.append($quantitySection);
        }
        return $ret;
    }

    drugOrderWidget.resetWidget = function(data) {
        if (data.values && data.values.length > 0) {
            data.values.forEach(function (val) {
                var config = data.config;
                var fieldSuffix = val.fieldSuffix;
                var action = val.action;
                var $actionButton = $('#order-action-button-' + action + fieldSuffix);
                $actionButton.click();
                val.fields.forEach(function (field) {
                    setValueByName(field.name, field.value);
                });
            });
            $('.order-field-widget.order-concept').find('select').change();
        }
    }

    drugOrderWidget.convertToAutocomplete = function($selectListElement) {
        // Create a new jQuery autocomplete text box, use it to update select list, which is hidden
        var options = [];
        $selectListElement.find('option').each(function() {
            var val = $(this).val();
            if (val !== '') {
                options.push( { 'label': $(this).html(), 'value': val })
            }
        });
        var $inputBox = $('<input type="text" autocomplete="do-not-fill" data-lpignore="true">');
        var $clearButton = $('<div class="custom-button">X</div>');
        $inputBox.css("width", $selectListElement.width());
        $inputBox.autocomplete({
            source: options,
            minChars: 0,
            width: $selectListElement.width(),
            matchContains: true,
            select: function(event, ui) {
                event.preventDefault();
                $inputBox.val(ui.item.label);
                $selectListElement.val(ui.item.value);
                $selectListElement.change();

            },
            focus: function(event, ui) {
                event.preventDefault();
                $inputBox.val(ui.item.label);
            },
        }).blur(function() {
            $inputBox.val($selectListElement.find("option:selected").html());
        });
        $clearButton.click(function() {
            $inputBox.val("").focus();
            $selectListElement.val("");
        })
        $inputBox.insertAfter($selectListElement);
        $clearButton.insertAfter($inputBox);
        $selectListElement.hide();
    }

}( window.drugOrderWidget = window.drugOrderWidget || {}, jQuery ));

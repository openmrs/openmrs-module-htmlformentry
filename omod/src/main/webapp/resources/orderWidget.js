//Uses the namespace pattern from http://stackoverflow.com/a/5947280
(function( orderWidget, $, undefined) {

    orderWidget.getEncounterDate = function(defaultDate) {
        var $encDateHidden = $('#encounterDate').find('input[type="hidden"]');
        var encDate = $encDateHidden.val();
        if (!encDate || encDate === '') {
            encDate = defaultDate;
        }
        return encDate;
    }

    orderWidget.getOrder = function(orderId, history) {
        var ret = null;
        history.forEach(function(order) {
            if (order.orderId === orderId) {
                ret =  order;
            }
        });
        return ret;
    }

    orderWidget.getConfigForConcept = function(config, conceptId) {
        var ret = null;
        config.concepts.forEach(function(concept) {
            if (concept.conceptId === conceptId) {
                ret = concept;
            }
        });
        return ret;
    }

    orderWidget.nextOrderableSectionIndex = function() {
        return $('.orderwidget-orderable-section').length;
    }

    orderWidget.nextActionButtonIndex = function() {
        return $('.order-action-button').length;
    }

    orderWidget.isOrderInCurrentEncounter = function(order, config) {
        return order.encounterId === config.encounterId
    }

    /**
     * Helper method which returns true if the given order is active on the given date
     */
    orderWidget.isOrderActive = function(order, onDate) {
        if (order.dateActivated.value > onDate) {
            return false;
        }
        if (order.effectiveStopDate.value !== '' && order.effectiveStopDate.value <= onDate) {
            return false;
        }
        return true;
    }

    // If the order was placed in this encounter, and it was a revision of a previous order, render the previous order
    orderWidget.shouldRenderPreviousOrder = function(order, config) {
        return orderWidget.isOrderInCurrentEncounter(order, config) && (order.previousOrderId !== '');
    }

    /* If the order was placed in this encounter, or if is active at the time of the encounter, then render it
       We also check if dateStopped is null.  This will result in hiding these if a historical encounter is edited and the order has been subsequently closed.
     */
    orderWidget.shouldRenderOrder = function(order, config, encDate) {
        return orderWidget.isOrderInCurrentEncounter(order, config) || (
            config.mode !== 'VIEW' && orderWidget.isOrderActive(order, encDate) && order.dateStopped.value === ""
        );
    }

    // If an order has subsequently been revised, do not allow editing
    orderWidget.canEditOrder = function(order, config, encDate) {
        if (orderWidget.shouldRenderOrder(order, config, encDate)) {
            config.history.forEach(function (historyOrder) {
                if (historyOrder.previousOrderId === order.orderId) {
                    return false;
                }
            });
            return true;
        }
        return false;
    }

    // Renew is allowed for active orders that are not placed in the same encounter
    orderWidget.canRenewOrder = function(order, config) {
        var ret = orderWidget.supportsAction(config, 'RENEW');
        ret = ret && order.action.value !== 'DISCONTINUE';
        ret = ret && (!orderWidget.isOrderInCurrentEncounter(order, config) || order.action.value === 'RENEW');
        return ret;
    }

    // Revise is allowed for active orders.  This is treated as a void/new if the encounter is the same on the backend
    orderWidget.canReviseOrder = function(order, config) {
        var ret = orderWidget.supportsAction(config, 'REVISE');
        ret = ret && order.action.value !== 'DISCONTINUE';
        ret = ret && (!orderWidget.isOrderInCurrentEncounter(order, config) || ['NEW','REVISE'].includes(order.action.value));
        return ret;
    }

    // Discontinue is allowed for active orders.
    orderWidget.canDiscontinueOrder = function(order, config) {
        var supportsAction = orderWidget.supportsAction(config, 'DISCONTINUE');
        var inEncounter = orderWidget.isOrderInCurrentEncounter(order, config);
        var notDiscontinued = order.action.value !== 'DISCONTINUE'
        return supportsAction && (inEncounter || notDiscontinued);
    }

    orderWidget.getActionOption = function(config, action) {
        var $orderTemplate = $('#' + config.fieldName + '_template');
        var $actionSection = $orderTemplate.find('.order-field.order-action');
        var $actionWidget = $actionSection.find('select');
        var $optionElement = $actionWidget.find('option[value="' + action + '"]');
        return $optionElement;
    }

    orderWidget.supportsAction = function(config, action) {
        return orderWidget.getActionOption(config, action).length > 0;
    }

    /**
     * Default function called by the <orders> tag tag to initialize and render the contents in all modes
     * Primary purpose is to ensure each configured section is re-rendered on load and on encounter date change
     */
    orderWidget.initialize = function(config) {

        console.log(config);

        // Get the section containing the html for this section
        var $widgetField = $('#' + config.fieldName);

        // Initialize all of the order sections that are available for revision
        orderWidget.renderOrdersForRevision(config);

        // Set up watch for an encounter date change.  If date changes, re-initialize all drug sections
        var $encDateHidden = $('#encounterDate').find('input[type="hidden"]');
        $encDateHidden.change(function() {
            if ($('.orderwidget-order-form').length > 1) { // More than 1, since the template is present
                alert(config.translations.encounterDateChangeWarning);
            }
            orderWidget.renderOrdersForRevision(config);
        });

        // Render ordering actions
        if (config.mode !== 'VIEW') {
            var $newOrderSection = orderWidget.createNewOrderSection(config);
            $widgetField.find('.orderwidget-selector-section').append($newOrderSection);
        }
    }

    orderWidget.renderOrdersForRevision = function(config) {
        var $widgetField = $('#' + config.fieldName);
        var encDate = orderWidget.getEncounterDate(config.defaultDate);

        var $orderSection = $widgetField.find(".orderwidget-order-section");
        $orderSection.empty();
        var $existingOrderSection = $('<div class="orderwidget-existing-order-section"></div>');

        var existingOrdersTitle = config.mode === 'VIEW' ? config.translations.existingOrdersViewTitle : config.translations.existingOrdersEditTitle;
        var newOrdersTitle = config.translations.newOrdersTitle;
        var noOrdersTitle = config.translations.noOrders;

        $existingOrderSection.append('<div class="orderwidget-section-header">' + existingOrdersTitle + '</div>');
        $existingOrderSection.append('<div class="orderwidget-section-no-orders">' + noOrdersTitle + '</div>');
        $orderSection.append($existingOrderSection);

        var $newOrderSection = $('<div class="orderwidget-new-order-section"></div>');
        $newOrderSection.append('<div class="orderwidget-section-header">' + newOrdersTitle + '</div>');
        $newOrderSection.append('<div class="orderwidget-section-no-orders">' + noOrdersTitle + '</div>');
        $orderSection.append($newOrderSection);

        config.history.forEach(function(order) {
            if (orderWidget.shouldRenderOrder(order, config, encDate)) {
                var newInEncounter = orderWidget.isOrderInCurrentEncounter(order, config) && order.action.value === 'NEW';
                if (newInEncounter) {
                    orderWidget.renderOrder($newOrderSection, order, config, encDate);
                }
                else {
                    orderWidget.renderOrder($existingOrderSection, order, config, encDate);
                }
            }
        });
    }

    orderWidget.addOrderSection = function($sectionToAddTo, config) {
        $sectionToAddTo.find(".orderwidget-section-no-orders").hide();
        var orderableSectionId = 'orderwidget-orderable-section-' + orderWidget.nextOrderableSectionIndex();
        var $orderableSection = $('<div id="' + orderableSectionId + '" class="orderwidget-orderable-section"></div>');
        $sectionToAddTo.append($orderableSection);
        return $orderableSection;
    }

    /**
     * Renders a given drug order
     */
    orderWidget.renderOrder = function($sectionToAppendTo, order, config, encDate) {

        var $orderableSection = orderWidget.addOrderSection($sectionToAppendTo, config);
        var $historySection = $('<div class="orderwidget-history-section"></div>');
        $orderableSection.append($historySection);
        $historySection.empty();

        // If the order was placed in this encounter, and it was a revision of a previous order, render the previous order
        if (orderWidget.shouldRenderPreviousOrder(order, config)) {
            var prevOrder = orderWidget.getOrder(order.previousOrderId, config.history);
            // If an order in this encounter was made to revise another encounter, show the encounter that was revised
            if (!orderWidget.isOrderInCurrentEncounter(prevOrder, config)) {
                var $prevOrderElement = orderWidget.formatOrder(prevOrder, encDate, config);
                $historySection.append($prevOrderElement);
            }
        }

        var $orderElement = orderWidget.formatOrder(order, encDate, config);
        $historySection.append($orderElement);

        if (config.mode !== 'VIEW') {
            var $actionSection = orderWidget.createEditOrderSections(order, config, encDate);
            $orderableSection.append($actionSection);
        }

        // Ensure this section is visible
        $orderableSection.show();
        return $orderableSection;
    }

    orderWidget.getSupportedActions = function(order, config, encDate) {
        var ret = [];
        if (orderWidget.canEditOrder(order, config, encDate)) {
            if (orderWidget.canRenewOrder(order, config)) {
                ret.push('RENEW');
            }
            if (orderWidget.canReviseOrder(order, config)) {
                ret.push('REVISE');
            }
            if (orderWidget.canDiscontinueOrder(order, config)) {
                ret.push('DISCONTINUE');
            }
        }
        return ret;
    }

    /**
     * Clones the order form template and replaces all the the ids and names as appropriate
     */
    orderWidget.constructOrderForm = function($sectionToAppendTo, idSuffix, config, action, $actionButton) {

        // Clone the order form template, ensuring ids and names of widgets are configured for this specific orderable
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
        $sectionToAppendTo.show();
        $sectionToAppendTo.append($orderForm);

        // Hide all widgets by default
        $orderForm.find('.order-field-label').hide();
        $orderForm.find('.order-field').hide();
        $orderForm.show();

        var encDate = orderWidget.getEncounterDate(config.defaultDate);

        // Ensure the action is set to the configured value
        var $actionSection = $orderForm.find('.order-field.order-action');
        var $actionWidget = $actionSection.find('select');
        $actionWidget.val(action);
        $actionWidget.hide();

        // Render the appropriate fields
        orderWidget.enableDateWidgets(config, $orderForm, encDate);

        if (action === 'DISCONTINUE') {
            var $discontinueReasonSelect = $orderForm.find('.order-field-widget.order-discontinueReason').find('select');
            if ($discontinueReasonSelect.find('option').length > 1) {
                $orderForm.find('.order-discontinueReason').show();
            }
            $orderForm.find('.order-discontinueReasonNonCoded').show();
        } else if (action === 'RENEW') {
            orderWidget.enableOrderDurationWidgets($orderForm);
        } else if (action === 'REVISE' || action === 'NEW') {
            $orderForm.find('.order-orderReason').show();
            orderWidget.enableOrderDoseWidgets($orderForm);
            $orderForm.find('.order-urgency').show();
            orderWidget.enableOrderDurationWidgets($orderForm);
        }

        // Set up ability to toggle between free-text and simple dosing instructions
        $orderForm.find('.order-dosingType').find('input:radio').change(function () {
            if (action === 'REVISE' || action === 'NEW') {
                orderWidget.enableOrderDoseWidgets($orderForm);
            }
        });

        // Set up ability to toggle between scheduled and non-scheduled urgencies
        $orderForm.find('.order-urgency').find('input:radio').change(function () {
            orderWidget.enableDateWidgets(config, $orderForm, encDate);
        });

        var $actionSection = $('<div class="order-cancel-action-section"></div>');
        var $deleteAction = $('<div class="order-action-button">' + config.translations.cancelAction + '</div>');
        $deleteAction.click(function(event) {
            $actionButton.removeClass('orderwidget-selected-action');
            $orderForm.remove();
            $sectionToAppendTo.hide();
        });
        $actionSection.append($deleteAction);
        $orderForm.append($actionSection);

        return $orderForm;
    }

    orderWidget.createActionButton = function(idSuffix, action, label) {
        return $('<div id="order-action-button-' + action + idSuffix + '" class="order-action-button">' + label + '</div>');
    }

    /**
     * The purpose of this function is to construct selectors and form for entering a NEW order
     */
    orderWidget.createNewOrderSection = function(config) {
        var $widgetField = $('#' + config.fieldName);
        var $actionOption = orderWidget.getActionOption(config, 'NEW');
        var $newButton = orderWidget.createActionButton("", 'NEW', $actionOption.html());
        $newButton.click(function () {
            var $newOrderSection = $widgetField.find('.orderwidget-new-order-section');
            var idSuffix = '_' + orderWidget.nextActionButtonIndex();
            var $orderSection = orderWidget.addOrderSection($newOrderSection, config);
            var $orderForm = orderWidget.constructOrderForm($orderSection, idSuffix, config, 'NEW', $newButton);
            orderWidget.enableOrderableSelector(config, $orderForm);
            orderWidget.enableContextWidgets(config, $orderForm);
            $orderForm.show();
        });
        return $newButton;
    }

    orderWidget.createEditOrderSections = function(order, config, encDate) {
        var $orderActionSection = $('<div class="order-action-section"></div>');
        var $orderActionButtons = $('<div class="order-action-buttons"></div>');
        $orderActionSection.append($orderActionButtons);
        var $orderActionForms = $('<div class="order-action-forms"></div>');
        $orderActionSection.append($orderActionForms);

        var orderActions = orderWidget.getSupportedActions(order, config, encDate);

        orderActions.forEach(function(action) {
            var $actionOption = orderWidget.getActionOption(config, action);
            var idSuffix = '_' + orderWidget.nextActionButtonIndex();

            var orderInCurrentEncounter = orderWidget.isOrderInCurrentEncounter(order, config);
            var isVoidAndEdit = orderInCurrentEncounter && (action === 'REVISE' && ['NEW','REVISE'].includes(order.action.value)) || (action === 'RENEW' && order.action.value === 'RENEW');
            var isVoid = (orderInCurrentEncounter && action === 'DISCONTINUE');

            var actionLabel = isVoidAndEdit ? config.translations.editOrder : isVoid ? config.translations.deleteOrder : $actionOption.html();

            var $actionButton = orderWidget.createActionButton(idSuffix, action, actionLabel);
            $actionButton.click(function() {
                var $orderForm = $orderActionForms.find('.orderwidget-order-form');
                $orderActionSection.find(".order-action-button").removeClass('orderwidget-selected-action');
                if ($orderForm.length > 0) {
                    $orderForm.remove();
                }
                $actionButton.addClass('orderwidget-selected-action');
                $actionButton.show();
                $orderForm = orderWidget.constructOrderForm($orderActionForms, idSuffix, config, action, $actionButton);
                orderWidget.populateOrderForm(config, $orderForm, order);

                if (isVoidAndEdit || isVoid) {
                    if (isVoid) {
                        $orderForm.find('.order-field').hide();
                    }
                    $orderForm.prepend("<div class='order-action-warnings'>" + config.translations.editDeleteWarning + "</div>");
                    $orderForm.show();
                }
            });
            $orderActionButtons.append($actionButton);
        });

        return $orderActionSection;
    }

    // Enable selecting formulations / drugs for the selected concept
    orderWidget.enableOrderableSelector = function(config, $orderForm) {
        var $conceptSelect = $orderForm.find('.order-field-widget.order-concept').find('select');
        var $drugSelect = $orderForm.find('.order-field-widget.order-drug').find('select');
        var $drugElements = $orderForm.find('.order-drug');
        var $drugNonCodedElements = $orderForm.find('.order-drugNonCoded');

        $orderForm.find('.order-concept').show();

        if ($conceptSelect.is(":visible")) {
            if (config.orderPropertyAttributes?.concept?.style === 'autocomplete') {
                orderWidget.convertToAutocomplete($conceptSelect);
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
                    var concept = orderWidget.getConfigForConcept(config, conceptId);
                    concept.drugs.forEach(function (drug) {
                        $drugSelect.find('option[value="' + drug.drugId + '"]').show();
                        $drugElements.show();
                    })
                    $drugNonCodedElements.show();
                }
            });
        }
        else {
            $drugElements.show();
            $drugNonCodedElements.hide();
            if (config.orderPropertyAttributes?.drug?.style === 'autocomplete') {
                orderWidget.convertToAutocomplete($drugSelect);
            }
        }
    }

    // If there was no template configured, show or set defaults where necessary
    orderWidget.enableContextWidgets = function(config, $orderForm) {
        if (config.hasTemplate === 'false') {
            $orderForm.find('.order-careSetting').show();
        }
        var $orderReasonSelect = $orderForm.find('.order-field-widget.order-orderReason').find(':input');
        if ($orderReasonSelect.find('option').length > 1) {
            $orderForm.find('.order-orderReason').show();
        }
    }

    orderWidget.enableDateWidgets = function(config, $orderForm, encDate) {

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

    orderWidget.enableOrderDoseWidgets = function($orderForm) {
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

    orderWidget.enableOrderDurationWidgets = function($orderForm) {
        $orderForm.find('.order-duration').show();
        $orderForm.find('.order-durationUnits').show();
        $orderForm.find('.order-quantity').show();
        $orderForm.find('.order-quantityUnits').show();
        $orderForm.find('.order-numRefills').show();
    }

    orderWidget.populateOrderForm = function(config, $orderForm, order) {
        $orderForm.find('.order-field-widget.order-previousOrder').find(':input').val(order.orderId);
        $orderForm.find('.order-field-widget.order-concept').find(':input').val(order.concept.value);
        $orderForm.find('.order-field-widget.order-orderReason').find(':input').val(order.orderReason.value);
        $orderForm.find('.order-field-widget.order-orderReasonNonCoded').find(':input').val(order.orderReasonNonCoded.value);
        $orderForm.find('.order-field-widget.order-careSetting').find(':input').val(order.careSetting.value);
        $orderForm.find('.order-field-widget.order-instructions').find(':input').val(order.instructions.value);
        if (order.isDrugOrder === 'true') {
            $orderForm.find('.order-field-widget.order-drug').find(':input').val(order.drug.value);
            $orderForm.find('.order-field-widget.order-drugNonCoded').find(':input').val(order.drugNonCoded.value);
            $orderForm.find('.order-field-widget.order-dosingType').find(':input[value="' + order.dosingType.value + '"]').click();
            $orderForm.find('.order-field-widget.order-orderType').find(':input').val(order.orderType.value);
            $orderForm.find('.order-field-widget.order-dosingInstructions').find(':input').val(order.dosingInstructions.value);
            $orderForm.find('.order-field-widget.order-dose').find(':input').val(order.dose.value);
            $orderForm.find('.order-field-widget.order-doseUnits').find(':input').val(order.doseUnits.value);
            $orderForm.find('.order-field-widget.order-route').find(':input').val(order.route.value);
            $orderForm.find('.order-field-widget.order-frequency').find(':input').val(order.frequency.value);
            if (order.asNeeded.value === 'true') {
                $orderForm.find('.order-field-widget.order-asNeeded').find(':input').attr('checked', 'true');
            }
            $orderForm.find('.order-field-widget.order-duration').find(':input').val(order.duration.value);
            $orderForm.find('.order-field-widget.order-durationUnits').find(':input').val(order.durationUnits.value);
            $orderForm.find('.order-field-widget.order-quantity').find(':input').val(order.quantity.value);
            $orderForm.find('.order-field-widget.order-quantityUnits').find(':input').val(order.quantityUnits.value);
            $orderForm.find('.order-field-widget.order-numRefills').find(':input').val(order.numRefills.value);
        }
    }

    orderWidget.formatOrder = function(d, encDate, config) {
        // Clone the view template, ensuring ids are configured for this specific orderable
        var $ret = $('#' + config.fieldName + '_view_template').clone();
        $ret.find("[id]").add($ret).each(function () {
            this.id = this.id + "_" + d.orderId;
        });

        var isActive = orderWidget.isOrderActive(d, encDate);
        $ret.addClass(isActive ? "order-view-active" : "order-view-inactive")

        var inCurrentEncounter = orderWidget.isOrderInCurrentEncounter(d, config);
        if (!inCurrentEncounter) {
            $ret.addClass('order-view-different-encounter');
        }
        else {
            $ret.addClass('order-view-current-encounter');
        }

        $ret.show();

        var orderableProperties = [
            'careSetting', 'concept', 'drug', 'drugNonCoded'
        ];
        var dosingProperties = [
            'dosingType', 'dose', 'doseUnits', 'route',
            'frequency', 'asNeeded', 'asNeededCondition', 'instructions', 'dosingInstructions'
        ];
        var dispensingProperties = [
            'quantity', 'quantityUnits', 'numRefills'
        ];
        var discontinueProperties = [
            'discontinueReason', 'discontinueReasonNonCoded'
        ];

        var hideForNew = [...discontinueProperties, 'action'];
        var hideForRevise = [...discontinueProperties];
        var hideForRenew = [...dosingProperties, ... discontinueProperties];
        var hideForDiscontinue = [
            ...dosingProperties, ...dispensingProperties,
            'orderReason', 'orderReasonNonCoded', 'duration', 'durationUnits'
        ]
        if (inCurrentEncounter) {
            hideForRevise = [...hideForRevise, ...orderableProperties];
            hideForRenew = [...hideForRenew, ...orderableProperties];
            hideForDiscontinue = [...hideForDiscontinue, ...orderableProperties];
        }
        else {
            hideForRevise.push('action');
            hideForRenew.push('action');
            hideForDiscontinue.push('action');
        }

        for (var orderProperty in d) {
            if (Object.prototype.hasOwnProperty.call(d, orderProperty)) {
                var val = d[orderProperty];
                if (val.display && val.display !== '') {
                    $ret.find('.order-field-widget.order-' + orderProperty).html(val.display);
                }
                else {
                    $ret.find('.order-field.order-' + orderProperty).addClass('order-field-hidden');
                }
                if (
                    (d.action.value === 'NEW' && hideForNew.includes(orderProperty)) ||
                    (d.action.value  === 'REVISE' && hideForRevise.includes(orderProperty)) ||
                    (d.action.value  === 'RENEW' && hideForRenew.includes(orderProperty)) ||
                    (d.action.value  === 'DISCONTINUE' && hideForDiscontinue.includes(orderProperty))
                ) {
                    $ret.find('.order-field.order-' + orderProperty).addClass('order-field-hidden');
                }
            }
        }

        return $ret;
    }

    orderWidget.resetWidget = function(data) {
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

    orderWidget.convertToAutocomplete = function($selectListElement) {
        // Create a new jQuery autocomplete text box, use it to update select list, which is hidden
        var options = [];
        $selectListElement.find('option').each(function() {
            var val = $(this).val();
            if (val !== '') {
                options.push( { 'label': $(this).html(), 'value': val })
            }
        });
        var $inputBox = $('<input type="text" class="orderwidget-autocomplete-textbox" autocomplete="do-not-fill" data-lpignore="true">');
        var $clearButton = $('<div class="custom-button">X</div>');
        var width = $selectListElement.width() + 20;
        $inputBox.css("width", width);
        $inputBox.autocomplete({
            source: options,
            minChars: 0,
            minLength: 0,
            width: width,
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
        }).focus(function() {
            $(this).autocomplete('search', $(this).val());
        });
        $clearButton.click(function() {
            $inputBox.val("").focus();
            $selectListElement.val("");
        })
        $inputBox.insertAfter($selectListElement);
        $clearButton.insertAfter($inputBox);
        $selectListElement.hide();
    }

}( window.orderWidget = window.orderWidget || {}, jQuery ));

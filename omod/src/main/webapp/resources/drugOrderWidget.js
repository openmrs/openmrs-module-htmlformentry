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

    drugOrderWidget.isCheckbox = function(config) {
        return config.tagAttributes && config.tagAttributes.checkbox && config.tagAttributes.checkbox === 'true';
    }

    drugOrderWidget.isSelectDrugs = function(config) {
        return !drugOrderWidget.isCheckbox(config);
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

    drugOrderWidget.getDrugConfig = function(config, drugId) {
        var ret = null;
        config.drugs.forEach(function(drug) {
            if (drug.drugId === drugId) {
                ret = drug;
            }
        });
        return ret;
    }

    drugOrderWidget.drugsAlreadyAdded = function(config) {
        return $('.drugorders-drug-section').length > 0;
    }

    drugOrderWidget.drugAlreadyAdded = function(config, drugId) {
        return $('#drugorders-drug-section-' + drugId).length > 0;
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

    /**
     * Default function called by the <drugOrders> tag tag to initialize and render the contents in all modes
     * Primary purpose is to ensure each configured drug section is re-rendered on load and on encounter date change
     */
    drugOrderWidget.initialize = function(config) {

        console.log(config);

        // Get the section containing the html for this section
        var $widgetField = $('#' + config.fieldName);

        // If config format is select and mode is not VIEW, render a drop down widget of drugs
        if (drugOrderWidget.isSelectDrugs(config)) {
            if (config.mode !== 'VIEW') {
                var $drugSelector = drugOrderWidget.buildDrugSelector(config);
                $widgetField.find('.drugorders-selector-section').append($drugSelector);
            }
        }

        // Initialize all of the drug sections
        drugOrderWidget.refreshDrugs(config);

        // Set up watch for an encounter date change.  If date changes, re-initialize all drug sections
        var $encDateHidden = $('#encounterDate').find('input[type="hidden"]');
        $encDateHidden.change(function() {
            if (drugOrderWidget.drugsAlreadyAdded(config)) {
                alert(config.translations.encounterDateChangeWarning);
            }
            drugOrderWidget.refreshDrugs(config);
        });
    }

    drugOrderWidget.refreshDrugs = function(config) {
        var $widgetField = $('#' + config.fieldName);
        $widgetField.find(".drugorders-order-section").empty();

        drugOrderWidget.getDrugsToRender(config).forEach(function(drug) {
            drugOrderWidget.configureDrugOrderWidget(config, drug);
        });
    }

    /**
     * The purpose of this function is to construct a select list of drugs that will
     * add a configured drug section to the form when a drug is selected
     */
    drugOrderWidget.buildDrugSelector = function(config) {
        var $drugSelector = $('<select class="drugorders-drug-selector"></select>');
        var label = (config.selectLabel ? config.selectLabel : config.translations.chooseDrug + "...");
        $drugSelector.append('<option value="">' + label + '</option>');
        config.drugs.forEach(function (drug) {
            $drugSelector.append('<option value="' + drug.drugId + '">' + drug.drugLabel + '</option>');
        });
        $drugSelector.change(function () {
            var drugId = $(this).val();
            if (drugId !== '') {
                var drug = drugOrderWidget.getDrugConfig(config, drugId);
                if (!drugOrderWidget.drugAlreadyAdded(config, drugId)) {
                    drugOrderWidget.configureDrugOrderWidget(config, drug);
                }
                $(this).val('');
            }
        });
        return $drugSelector;
    }

    /**
     * The purpose of this function is to determine what drugs should be rendered when the widget is reloaded,
     * either as a result of an initial page load or as a result of an encounter date change
     * It returns all configured drugs, if the widget is configured to display all drugs by default
     * It will always return those drugs that have existing drugOrders within the current encounter
     * If not in view mode, it will also return those drugs that have active drug orders to facilitate
     * operating on them in the current encounter by default
     */
    drugOrderWidget.getDrugsToRender = function(config) {
        var ret = new Array();
        var encDate = drugOrderWidget.getEncounterDate(config.defaultDate);
        config.drugs.forEach(function(drugConfig) {
            if (drugOrderWidget.isSelectDrugs(config)) {
                var drugHistory = drugConfig.history ? drugConfig.history : new Array();
                var renderDrug = drugOrderWidget.drugAlreadyAdded(config, drugConfig.drugId);
                if (!renderDrug) {
                    drugHistory.forEach(function (drugOrder) {
                        if (drugOrderWidget.isOrderInCurrentEncounter(drugOrder, config)) {
                            renderDrug = true;
                        }
                        else if (config.mode !== 'VIEW' && drugOrderWidget.isOrderActive(drugOrder, encDate)) {
                            renderDrug = true;
                        }
                    });
                }
                if (renderDrug) {
                    ret.push(drugConfig);
                }
            }
            else {
                ret.push(drugConfig);
            }
        });
        return ret;
    }

    /**
     * The purpose of this function is to re-initialize a given drug order section, in response to the mode
     * and to interactions on the form (encounter date changes, action changes, etc).
     */
    drugOrderWidget.configureDrugOrderWidget = function(config, drugConfig) {
        var $elementSection = $('#' + config.fieldName);
        var $ordersSection = $elementSection.find('.drugorders-order-section');

        var $drugSection = $('<div id="drugorders-drug-section-' + drugConfig.drugId + '" class="drugorders-drug-section"></div>');
        var $drugDetailsSection = $('<div class="drugorders-drug-details"></div>');
        var $historySection = $('<div class="drugorders-drug-history"></div>');

        $ordersSection.append($drugSection);
        $drugSection.append($drugDetailsSection);
        $drugSection.append($historySection);

        // Render drug name details
        $drugDetailsSection.html(drugConfig.drugLabel);

        // Render drug history details

        // Start by removing all of the existing elements
        $historySection.empty();

        // If there are existing orders in the encounter, render
        // Also render any order from a previous encounter that was revised by an order in the current encounter
        var lastRenderedOrder = null;
        var lastOrderInEncounter = null;

        var encDate = drugOrderWidget.getEncounterDate(config.defaultDate);
        var drugHistory = drugConfig.history ? drugConfig.history : new Array();

        drugHistory.forEach(function(drugOrder) {
            if (drugOrderWidget.isOrderInCurrentEncounter(drugOrder, config)) {
                if (drugOrder.previousOrderId !== '') {
                    var prevOrder = drugOrderWidget.getOrder(drugOrder.previousOrderId, drugConfig.history);
                    if (!drugOrderWidget.isOrderInCurrentEncounter(prevOrder, config)) {
                        var $prevOrderElement = drugOrderWidget.formatDrugOrder(prevOrder, encDate, config);
                        $historySection.append($prevOrderElement);
                    }
                }
                var $orderElement = drugOrderWidget.formatDrugOrder(drugOrder, encDate, config);
                $historySection.append($orderElement);
                lastRenderedOrder = drugOrder;
                lastOrderInEncounter = drugOrder;

                // Ensure this section is visible
                $drugSection.show();
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
                    if (drugOrderWidget.isOrderActive(drugOrder, encDate)) {
                        activeOrder = drugOrder;
                    }
                });
                if (activeOrder != null) {
                    var $lastActiveElement = drugOrderWidget.formatDrugOrder(activeOrder, encDate, config);
                    $historySection.append($lastActiveElement);
                    lastRenderedOrder = activeOrder;
                }
            }
        }

        // If the form is in entry or edit mode, add the option to enter orders

        if (config.mode !== 'VIEW') {

            // Clone the order form template, ensuring ids and names of widgets are configured for this specific drug

            var $orderForm = $('#' + config.fieldName + '_template').clone();
            var idSuffix = '_' + drugConfig.drugId;
            $orderForm.find("[id]").add($orderForm).each(function () {
                this.id = this.id + idSuffix;
            });
            $orderForm.find("[name]").add($orderForm).each(function () {
                this.name = this.name + idSuffix;
            });

            // This is necessary to ensure each NumberFieldWidget interacts with the associated, cloned errorWidget
            $orderForm.find('input[onblur*=checkNumber]').attr('onblur', function(index, currentValue) {
                var split = currentValue.split(/(checkNumber\(this,')(w\d*)('*)/);
                var newFn = '';
                split.forEach(function(val) {
                    if (RegExp(/w\d*/).test(val)) {
                        val = val + idSuffix;
                    }
                    newFn += val;
                })
                return newFn;
            });

            $drugSection.append($orderForm);

            // Pre-populate the order form with the last rendered order if applicable
            if (lastRenderedOrder) {
                drugOrderWidget.populateOrderForm(config, $orderForm, lastRenderedOrder);
            }

            // Show edit section, hide all widgets
            $orderForm.find('.order-field-label').hide();
            $orderForm.find('.order-field').hide();
            $orderForm.show();

            // Set up allowed actions
            // Initially, we support a limited set of actions due to complexity of retrospective data entry of orders

            var $actionSection = $orderForm.find('.order-field.action');
            var $actionWidget = $actionSection.find('select');
            $actionWidget.find('option').hide();

            var d = lastRenderedOrder;
            var lastStart = (lastRenderedOrder != null ? lastRenderedOrder.effectiveStartDate.value : '');
            var lastStop = (lastRenderedOrder != null ? lastRenderedOrder.effectiveStopDate.value : '');

            var allowedActions = [];
            allowedActions.push("");

            // Allow drugs to be ordered NEW if there are no orders active on or after the encounter date
            if (lastStart === '' || (lastStop !== '' && lastStop <= encDate)) {
                allowedActions.push("NEW");
            }

            // Only RENEW, REVISE, or DISCONTINUE are possible with existing orders
            if (lastStart !== '') {
                // Don't allow any further revisions to a DISCONTINUE order
                if (lastRenderedOrder.action.value !== 'DISCONTINUE') {
                    // Allow REVISION and DISCONTINUE if operating on an order with the same or an earlier start date
                    if (lastStart <= encDate) {
                        allowedActions.push("REVISE");
                        allowedActions.push("DISCONTINUE");
                    }
                    // Allow RENEW if operating on an order with an earlier start date
                    if (lastStart < encDate) {
                        allowedActions.push("RENEW");
                    }
                }
            }

            allowedActions.forEach(function (action) {
                var $optionElement = $actionWidget.find('option[value="' + action + '"]');
                $optionElement.attr('selected', false);
                $optionElement.show();
            });

            if ($actionWidget.find('option').length > 0) {
                $actionWidget.change(function () {
                    var action = this.value;
                    $orderForm.find('.order-field').hide();
                    $orderForm.find('.order-field.action').show();
                    if (action !== '') {
                        drugOrderWidget.enableDateWidgets(config, $orderForm, encDate);
                    }
                    if (action === 'NEW') {
                        drugOrderWidget.enableContextWidgets(config, $orderForm)
                    }
                    if (action === 'DISCONTINUE') {
                        $orderForm.find('.discontinueReason').show();
                    } else if (action === 'RENEW') {
                        drugOrderWidget.enableDrugOrderDurationWidgets($orderForm);
                    } else if (action === 'REVISE' || action === 'NEW') {
                        drugOrderWidget.enableDrugOrderDoseWidgets($orderForm);
                        $orderForm.find('.urgency').show();
                        drugOrderWidget.enableDrugOrderDurationWidgets($orderForm);
                    }
                });
                $actionSection.show();
                $actionSection.children().show();
            }

            // If there is only one action configured (in addition to empty action),
            // and there are no existing orders for the drug in the encounter, toggle it by default
            if (!drugOrderWidget.isCheckbox(config)) {
                if (allowedActions.length === 2 && lastOrderInEncounter === null) {
                    $actionWidget.val(allowedActions[1]);
                } else {
                    $actionWidget.val(allowedActions[0]);
                }
                $actionWidget.change();
            }

            // Set up ability to toggle between free-text and simple dosing instructions

            $orderForm.find('.dosingType').find('input:radio').change(function () {
                drugOrderWidget.enableDrugOrderDoseWidgets($orderForm);
            });

            // Set up ability to toggle between scheduled and non-scheduled urgencies
            $orderForm.find('.urgency').find('input:radio').change(function () {
                drugOrderWidget.enableDateWidgets(config, $orderForm, encDate);
            });
        }
    }

    // If there was no template configured, show or set defaults where necessary
    drugOrderWidget.enableContextWidgets = function(config, $orderForm) {
        if (config.hasTemplate === 'false') {
            $orderForm.find('.careSetting').show();
            $orderForm.find('.orderType').show();
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
        $orderForm.find('.dateActivated').show();
        var $dateActivatedSection = $orderForm.find('.order-field.dateActivated');
        var $dateActivatedWidget = $dateActivatedSection.find('.order-field-widget.dateActivated');
        var $dateActivatedTextField = $dateActivatedWidget.find('input[type=text]');
        setDatePickerValue($dateActivatedTextField, (encDate));
        $dateActivatedWidget.hide();
        $dateActivatedSection.find('.value').remove();
        $dateActivatedSection.append('<span class="value">' + $dateActivatedTextField.val() + '</span>')
        $dateActivatedSection.show();

        // Allow scheduled date to be set
        var $urgencySection = $orderForm.find('.urgency');
        var urgencyVal = $urgencySection.find('input:checked').val();
        if (urgencyVal === 'ON_SCHEDULED_DATE') {
            $orderForm.find('.scheduledDate').show();
        }
        else {
            $orderForm.find('.scheduledDate').hide();
        }
    };

    drugOrderWidget.enableDrugOrderDoseWidgets = function($orderForm) {
        var $dosingTypeSection = $orderForm.find('.dosingType');
        var dosingTypeVal = $dosingTypeSection.find('input:checked').val();
        if (dosingTypeVal === 'org.openmrs.FreeTextDosingInstructions') {
            $orderForm.find('.dose').hide();
            $orderForm.find('.doseUnits').hide();
            $orderForm.find('.frequency').hide();
            $orderForm.find('.route').hide();
            $orderForm.find('.asNeeded').hide();
            $orderForm.find('.instructions').hide();
            $orderForm.find('.dosingInstructions').show();
        }
        else {
            $orderForm.find('.dose').show();
            $orderForm.find('.doseUnits').show();
            $orderForm.find('.frequency').show();
            $orderForm.find('.route').show();
            $orderForm.find('.asNeeded').show();
            $orderForm.find('.instructions').show();
            $orderForm.find('.dosingInstructions').hide();
        }
        $dosingTypeSection.show();
    }

    drugOrderWidget.enableDrugOrderDurationWidgets = function($orderForm) {
        $orderForm.find('.duration').show();
        $orderForm.find('.durationUnits').show();
        $orderForm.find('.quantity').show();
        $orderForm.find('.quantityUnits').show();
        $orderForm.find('.numRefills').show();
    }

    drugOrderWidget.populateOrderForm = function(config, $orderForm, drugOrder) {
        $orderForm.find('.order-field-widget.previousOrder').find(':input').val(drugOrder.orderId);
        $orderForm.find('.order-field-widget.careSetting').find(':input').val(drugOrder.careSetting.value);
        $orderForm.find('.order-field-widget.dosingType').find(':input[value="' + drugOrder.dosingType.value + '"]').click();
        $orderForm.find('.order-field-widget.orderType').find(':input').val(drugOrder.orderType.value);
        $orderForm.find('.order-field-widget.dosingInstructions').find(':input').val(drugOrder.dosingInstructions.value);
        $orderForm.find('.order-field-widget.dose').find(':input').val(drugOrder.dose.value);
        $orderForm.find('.order-field-widget.doseUnits').find(':input').val(drugOrder.doseUnits.value);
        $orderForm.find('.order-field-widget.route').find(':input').val(drugOrder.route.value);
        $orderForm.find('.order-field-widget.frequency').find(':input').val(drugOrder.frequency.value);
        if (drugOrder.asNeeded.value === 'true') {
            $orderForm.find('.order-field-widget.asNeeded').find(':input').attr('checked', 'true');
        }
        $orderForm.find('.order-field-widget.instructions').find(':input').val(drugOrder.instructions.value);
        $orderForm.find('.order-field-widget.duration').find(':input').val(drugOrder.duration.value);
        $orderForm.find('.order-field-widget.durationUnits').find(':input').val(drugOrder.durationUnits.value);
        $orderForm.find('.order-field-widget.quantity').find(':input').val(drugOrder.quantity.value);
        $orderForm.find('.order-field-widget.quantityUnits').find(':input').val(drugOrder.quantityUnits.value);
        $orderForm.find('.order-field-widget.numRefills').find(':input').val(drugOrder.numRefills.value);
    }

    drugOrderWidget.formatDrugOrder = function(d, encDate, config) {
        var $ret = $('<div class="drugorders-order-history-item"></div>');

        var $existingActionSection = $('<div class="order-view-section order-view-action"></div>');
        var inCurrentEncounter = drugOrderWidget.isOrderInCurrentEncounter(d, config);
        if (!inCurrentEncounter) {
            $ret.addClass('order-view-different-encounter');
            $existingActionSection.append(config.translations.previousOrder);
        }
        else {
            $ret.addClass('order-view-current-encounter');
            $ret.addClass('value');
            $existingActionSection.append(d.action.display);
        }
        var isActive = drugOrderWidget.isOrderActive(d, encDate);
        $ret.addClass(isActive ? "order-view-active" : "order-view-inactive")
        $ret.append($existingActionSection);

        var isDiscontinue = (d.action.value === 'DISCONTINUE');

        var $dateSection = $('<div class="order-view-section order-view-dates"></div>');
        $dateSection.append('<div class="order-view-field order-view-start-date">');
        $dateSection.append(config.translations.starting + ' ' + d.effectiveStartDate.display);
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
            $discontinueSection.append('<div class="order-view-field order-view-discontinue-reason">' + d.discontinueReason.display + '</div>');
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
                var drugId = val.drugId;
                var drug = drugOrderWidget.getDrugConfig(config, drugId);
                if (!drugOrderWidget.drugAlreadyAdded(config, drugId)) {
                    drugOrderWidget.configureDrugOrderWidget(config, drug);
                    val.fields.forEach(function (field) {
                        setValueByName(field.name, field.value);
                    });
                }
            });
        }
    }

}( window.drugOrderWidget = window.drugOrderWidget || {}, jQuery ));

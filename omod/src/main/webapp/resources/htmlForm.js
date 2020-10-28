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

    htmlForm.getEncounterDate = function(defaultDate) {
        var $encDateHidden = $('#encounterDate').find('input[type="hidden"]');
        var encDate = $encDateHidden.val();
        if (!encDate || encDate === '') {
            encDate = defaultDate;
        }
        return encDate;
    }

    htmlForm.isSelectDrugs = function(config) {
        return config.format && config.format === 'select';
    }

    /**
     * Default function called by the <drugOrders> tag tag to initialize and render the contents in all modes
     * Primary purpose is to ensure each configured drug section is re-rendered on load and on encounter date change
     */
    htmlForm.initializeDrugOrdersWidgets = function(config) {

        console.log(config);

        // Get the section containing the html for this section
        var $widgetField = $('#' + config.fieldName);

        // If config format is select and mode is not VIEW, render a drop down widget of drugs
        if (htmlForm.isSelectDrugs(config)) {
            if (config.mode !== 'VIEW') {
                var $drugSelector = htmlForm.buildDrugSelector(config);
                $widgetField.find('.drugorders-header-section').append($drugSelector);
            }
            // For any drugs with existing orders, render these
            htmlForm.getDrugsToRender(config).forEach(function(drug) {
                htmlForm.configureDrugOrderWidget(config, null, drug);
            });
        }
        // Otherwise, render sections for all configured drugs
        else {
            config.drugs.forEach(function(drug) {
                htmlForm.configureDrugOrderWidget(config, null, drug);
            });
        }
    }

    /**
     * The purpose of this function is to construct a select list of drugs that will
     * add a configured drug section to the form when a drug is selected
     */
    htmlForm.buildDrugSelector = function(config) {
        var $drugSelector = $('<select class="drugorders-drug-selector"></select>');
        var label = (config.selectLabel ? config.selectLabel : config.translations.chooseDrug + "...");
        $drugSelector.append('<option value="">' + label + '</option>');
        config.drugs.forEach(function (drug) {
            $drugSelector.append('<option value="' + drug.drugId + '">' + drug.drugLabel + '</option>');
        });
        $drugSelector.change(function () {
            var drugId = $(this).val();
            if (drugId !== '') {
                var drug = htmlForm.getDrugConfig(config, drugId);
                if (!htmlForm.drugAlreadyAdded(config, drugId)) {
                    htmlForm.configureDrugOrderWidget(config, null, drug);
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
     * Otherwise it returns those drugs that have existing drugOrders within the current encounter
     */
    htmlForm.getDrugsToRender = function(config) {
        var ret = new Array();
        config.drugs.forEach(function(drugConfig) {
            if (htmlForm.isSelectDrugs(config)) {
                var drugHistory = drugConfig.history ? drugConfig.history : new Array();
                var renderDrug = false;
                drugHistory.forEach(function(drugOrder) {
                    if (htmlForm.isOrderInCurrentEncounter(drugOrder, config)) {
                        renderDrug = true;
                    }
                });
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
    htmlForm.configureDrugOrderWidget = function(config, action, drugConfig) {
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

        var encDate = htmlForm.getEncounterDate(config.today);
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

        // If the form is in entry or edit mode, add the option to enter orders

        if (config.mode !== 'VIEW') {

            // Clone the order form template, ensuring ids and names of widgets are configured for this specific drug

            var $orderForm = $('#' + config.fieldName + '_template').clone();
            var idSuffix = '_' + drugConfig.drugId;
            $orderForm.find("[id]").add($orderForm).each(function() {
                this.id = this.id + idSuffix;
            });
            $orderForm.find("[name]").add($orderForm).each(function() {
                this.name = this.name + idSuffix;
            });
            $drugSection.append($orderForm);

            // Pre-populate the order form with the last rendered order if applicable
            if (lastRenderedOrder) {
                htmlForm.populateOrderForm(config, $orderForm, lastRenderedOrder);
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
                    $orderForm.find('.order-field').hide();
                    $orderForm.find('.order-field.action').show();
                    if (action !== '') {
                        htmlForm.enableDateWidgets(config, $orderForm, encDate);
                    }
                    if (action === 'DISCONTINUE') {
                        $orderForm.find('.discontinueReason').show();
                    }
                    else if (action === 'RENEW') {
                        htmlForm.enableDrugOrderDurationWidgets($orderForm);
                    }
                    else if (action === 'REVISE' || action === 'NEW') {
                        htmlForm.enableDrugOrderDoseWidgets($orderForm);
                        $orderForm.find('.urgency').show();
                        htmlForm.enableDrugOrderDurationWidgets($orderForm);
                    }
                });
                $actionSection.show();
                $actionSection.children().show();
            }

            // Set up ability to toggle between free-text and simple dosing instructions

            $orderForm.find('.dosingType').find('input:radio').change(function() {
                htmlForm.enableDrugOrderDoseWidgets($orderForm);
            });

            // Set up ability to toggle between scheduled and non-scheduled urgencies
            $orderForm.find('.urgency').find('input:radio').change(function() {
                htmlForm.enableDateWidgets(config, $orderForm, encDate);
            });

            // Set up watch for an encounter date change.  If date changes, rebuild this widget
            var $encDateHidden = $('#encounterDate').find('input[type="hidden"]');
            $encDateHidden.change(function() {
                htmlForm.configureDrugOrderWidget(config, null, drug);
            });
        }
    }

    htmlForm.enableDateWidgets = function(config, $orderForm, encDate) {

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

    htmlForm.enableDrugOrderDoseWidgets = function($orderForm) {
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

    htmlForm.enableDrugOrderDurationWidgets = function($orderForm) {
        $orderForm.find('.duration').show();
        $orderForm.find('.durationUnits').show();
        $orderForm.find('.quantity').show();
        $orderForm.find('.quantityUnits').show();
        $orderForm.find('.numRefills').show();
    }

    htmlForm.populateOrderForm = function(config, $orderForm, drugOrder) {
        $orderForm.find('.order-field-widget.careSetting').find(':input').val(drugOrder.careSetting.value);
        $orderForm.find('.order-field-widget.dosingType').find(':input').val(drugOrder.dosingType.value);
        $orderForm.find('.order-field-widget.orderType').find(':input').val(drugOrder.orderType.value);
        $orderForm.find('.order-field-widget.dosingInstructions').find(':input').val(drugOrder.dosingInstructions.value);
        $orderForm.find('.order-field-widget.dose').find(':input').val(drugOrder.dose.value);
        $orderForm.find('.order-field-widget.doseUnits').find(':input').val(drugOrder.doseUnits.value);
        $orderForm.find('.order-field-widget.route').find(':input').val(drugOrder.route.value);
        $orderForm.find('.order-field-widget.frequency').find(':input').val(drugOrder.frequency.value);
        if (drugOrder.asNeeded.value == 'true') {
            $orderForm.find('.order-field-widget.asNeeded').find(':input').attr('checked', 'true');
        }
        $orderForm.find('.order-field-widget.instructions').find(':input').val(drugOrder.instructions.value);
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

    htmlForm.getDrugConfig = function(config, drugId) {
        var ret = null;
        config.drugs.forEach(function(drug) {
            if (drug.drugId === drugId) {
                ret = drug;
            }
        });
        return ret;
    }

    htmlForm.drugAlreadyAdded = function(config, drugId) {
        return $('#drugorders-drug-section-' + drugId).length > 0;
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

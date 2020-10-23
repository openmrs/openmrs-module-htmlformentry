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

        var $historySection = $drugSection.find(".drugOrdersHistory");
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
                $historySection.append('<span>No Orders</span>'); // TODO: Translate this
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
            $editWidgetSection.find('.order-field').hide();
            $editWidgetSection.show();

            var $actionSection = $editWidgetSection.find('.order-field.action');
            var $actionWidget = $actionSection.find('select');
            $actionWidget.find('option').remove();

            var d = lastRenderedOrder;
            var lastStart = (lastRenderedOrder != null ? lastRenderedOrder.effectiveStartDate.value : '');
            var lastStop = (lastRenderedOrder != null ? lastRenderedOrder.effectiveStopDate.value : '');

            // Initially, we support a limited set of actions due to complexity of retrospective data entry of orders

            // Allow drugs to be ordered NEW if there are no orders active on or after the encounter date
            if (lastStart === '' || (lastStop !== '' && lastStop <= encDate)) {
                htmlForm.addOrderAction($actionWidget, 'NEW', d, encDate, config);
            }

            // Only RENEW, REVISE, or DISCONTINUE are possible with existing orders
            if (lastStart !== '') {
                // Don't allow any further revisions to a DISCONTINUE order
                if (lastRenderedOrder.action.value !== 'DISCONTINUE') {
                    // Allow REVISION operations if operating on an order with the same or an earlier start date
                    if (lastStart <= encDate) {
                        htmlForm.addOrderAction($actionWidget, 'REVISE', d, encDate, config);
                        htmlForm.addOrderAction($actionWidget, 'RENEW', d, encDate, config);
                        htmlForm.addOrderAction($actionWidget, 'DISCONTINUE', d, encDate, config);
                    }
                }
            }

            if ($actionWidget.find('option').length > 0) {
                $actionWidget.change(function() {
                   var action = this.value;
                    $editWidgetSection.find('.order-field').hide();
                    $editWidgetSection.find('.order-field.action').show();
                   if (action !== '') {
                       var $dateActivatedWidget = $editWidgetSection.find('.order-field.dateActivated');
                       setDatePickerValue($dateActivatedWidget.find('input[type=text]'), (encDate));
                       $dateActivatedWidget.show();
                   }
                   if (action === 'DISCONTINUE') {
                       $editWidgetSection.find('.order-field.discontinueReason').show();
                   }
                   else if (action === 'RENEW') {
                       $editWidgetSection.find('.order-field.duration').show();
                       $editWidgetSection.find('.order-field.durationUnits').show();
                       $editWidgetSection.find('.order-field.quantity').show();
                       $editWidgetSection.find('.order-field.quantityUnits').show();
                       $editWidgetSection.find('.order-field.numRefills').show();
                   }
                   else if (action === 'REVISE' || action === 'NEW') {
                       $editWidgetSection.find('.order-field.dose').show();
                       $editWidgetSection.find('.order-field.doseUnits').show();
                       $editWidgetSection.find('.order-field.frequency').show();
                       $editWidgetSection.find('.order-field.route').show();
                       $editWidgetSection.find('.order-field.asNeeded').show();
                       $editWidgetSection.find('.order-field.instructions').show();
                       $editWidgetSection.find('.order-field.dosingType').show();
                       $editWidgetSection.find('.order-field.urgency').show();
                       $editWidgetSection.find('.order-field.duration').show();
                       $editWidgetSection.find('.order-field.durationUnits').show();
                       $editWidgetSection.find('.order-field.quantity').show();
                       $editWidgetSection.find('.order-field.quantityUnits').show();
                       $editWidgetSection.find('.order-field.numRefills').show();
                   }
                });
                $actionSection.show();
            }
        }
    }

    // TODO: Use a view template from the htmlform configuration, and populate based on cssClass
    htmlForm.formatDrugOrder = function(d, encDate, config) {
        var $ret = $('<div class="order-history-item"></div>');

        var $existingActionSection = $('<span class="drugOrderActionView"></span>');
        var inCurrentEncounter = htmlForm.isOrderInCurrentEncounter(d, config);
        if (!inCurrentEncounter) {
            $ret.addClass('order-not-in-encounter');
            $existingActionSection.append('Previous Order'); // TODO: Translate this
        }
        else {
            $existingActionSection.append(d.action.display);
        }
        var isActive = htmlForm.isOrderActive(d, encDate);
        $ret.addClass(isActive ? "order-active" : "order-inactive")
        $ret.append($existingActionSection);

        var $dateSection = $('<span class="drug-order-view-dates"></span>');
        $dateSection.append('<span class="drugOrderStartDateView">' + d.effectiveStartDate.display + '</span>');
        var endDate = (d.effectiveStopDate.display === "" ? 'Present' : d.effectiveStopDate.display); // TODO: Translate
        $dateSection.append(' - <span class="drugOrderStopDateView">' + endDate + '</span>');
        $ret.append($dateSection);

        if (d.dose.display !== '') {
            $ret.append('<span class="drugOrderDoseView">' + d.dose.display + " " + d.doseUnits.display + "</span>");
        }
        if (d.route.display !== "") {
            $ret.append(' -- <span class="drugOrderRouteView">' + d.route.display + '</span>');
        }
        if (d.frequency.display !== "") {
            $ret.append(' -- <span class="drugOrderFrequencyView">' + d.frequency.display + '</span>');
        }
        if (d.asNeeded.value === "true") {
            $ret.append(' -- <span class="drugOrderAsNeededView">As Needed</span>'); // TODO: Translate
        }
        return $ret;
    }

    htmlForm.addOrderAction = function($actionSelect, actionType, d, encDate, config) {
        if ($actionSelect.find('option').length === 0) {
            $actionSelect.append($('<option value="">Action...</option>')); // TODO: Translate
        }
        $actionSelect.append($('<option value="' + actionType + '">' + actionType + '</option>'));
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

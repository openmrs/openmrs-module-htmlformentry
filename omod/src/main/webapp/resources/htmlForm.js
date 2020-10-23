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

        // If there are existing orders in the encounter, start out by rendering those, including previous order details
        var anyRendered = false;
        drugConfig.history.forEach(function(drugOrder) {
            var inEncounter = (config.encounterId === drugOrder.encounterId);
            if (inEncounter) {
                if (drugOrder.previousOrderId !== '') {
                    var previousOrder = htmlForm.getOrder(drugOrder.previousOrderId, drugConfig.history);
                    if (previousOrder.encounterId !== config.encounterId) {
                        var $prevOrderElement = htmlForm.formatDrugOrder(previousOrder, 'order-not-in-encounter');
                        $historySection.append($prevOrderElement);
                    }
                }
                var $orderElement = htmlForm.formatDrugOrder(drugOrder, 'order-in-encounter');
                $historySection.append($orderElement);
                anyRendered = true
            }
        });

        // If none were rendered, and mode is view, indicate no oders
        // If none were rendered, and mode is not view, render based on any active orders not in the current encounter
        if (!anyRendered) {
            if (config.mode === 'VIEW') {
                $historySection.append('<span>No Orders</span>'); // TODO: Translate this
            }
            else {
                // If there are any active orders on this date, render them
                var lastActiveOrder = null;
                drugConfig.history.forEach(function(drugOrder) {
                    if (htmlForm.isOrderActive(drugOrder, encDate)) {
                        lastActiveOrder = drugOrder;
                    }
                });
                if (lastActiveOrder != null) {
                    var $lastActiveElement = htmlForm.formatDrugOrder(lastActiveOrder, 'order-not-in-encounter');
                    $historySection.append($lastActiveElement);
                }
            }
        }
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

    htmlForm.formatDrugOrder = function(d, cssClass) {
        var $ret = $('<div class="' + cssClass + '"></div>');
        var $dateSection = $('<span class="drug-order-view-dates"></span>');
        $dateSection.append('<span class="drugOrderStartDateView">' + d.effectiveStartDate.display + '</span>');
        if (d.effectiveStopDate.display !== "") {
            $dateSection.append(' - <span class="drugOrderStopDateView">' + d.effectiveStopDate.display + '</span>');
        }
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
        return $ret;
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

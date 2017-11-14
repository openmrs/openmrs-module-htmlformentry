//Uses the namespace pattern from http://stackoverflow.com/a/5947280
(function( htmlForm, $, undefined) {

    var onObsChangedCheck = function() {
        var whenValueThenDisplaySection = $(this).data('whenValueThenDisplaySection');
         // handle differently autocomplete fields since the obs value is located on the hidden element
         var val = $(this).val();

         if ($(this).hasClass("ui-autocomplete-input")) {
                val = $("#"+$(this).attr("id")+"_hid").val();
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

    }

    htmlForm.compileMustacheTemplate = function(source) {
        return Handlebars.compile(source);
    };

    // any users of this library should call this function during page load to make sure that all elements are properly initialized
    // if new functionality is added that requires setup, the setup function should be called from here
    htmlForm.initialize = function() {
        htmlForm.setupObsToggleHandlers();
    }

}( window.htmlForm = window.htmlForm || {}, jQuery )); 
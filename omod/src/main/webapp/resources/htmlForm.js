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
                    $(thenSection).find('input:hidden, input:text, input:password, input:file, select, textarea').val('');
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

    htmlForm.setupWhenThen = function(obsId, valueToSection, valueToThenJs, valueToElseJs) {
        var field = getField(obsId + '.value');
        field.data('whenValueThenDisplaySection', valueToSection);
        field.data('whenValueThenJs', valueToThenJs);
        field.data('whenValueElseJs', valueToElseJs);
        field.change(onObsChangedCheck).change();
    };

    htmlForm.compileMustacheTemplate = function(source) {
        return Handlebars.compile(source);
    };

}( window.htmlForm = window.htmlForm || {}, jQuery )); 
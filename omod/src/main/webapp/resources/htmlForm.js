//Uses the namespace pattern from http://stackoverflow.com/a/5947280
(function( htmlForm, $, undefined) {

    var onObsChangedCheck = function() {
        var whenValueThenDisplaySection = $(this).data('whenValueThenDisplaySection');
        if (whenValueThenDisplaySection) {
            var val = $(this).val();
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
    };

    htmlForm.setupWhenThenDisplay = function(obsId, valueToSection) {
        var field = getField(obsId + '.value');
        field.data('whenValueThenDisplaySection', valueToSection);
        field.change(onObsChangedCheck).change();
    };

    htmlForm.compileMustacheTemplate = function(source) {
        return Handlebars.compile(source);
    };

}( window.htmlForm = window.htmlForm || {}, jQuery ));
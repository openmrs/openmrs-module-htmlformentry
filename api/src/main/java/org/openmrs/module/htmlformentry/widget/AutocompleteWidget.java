package org.openmrs.module.htmlformentry.widget;

import java.util.Iterator;
import java.util.List;

import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.springframework.web.util.HtmlUtils;

/**
 *   A single option auto complete widget which provides auto complete suggestions using a
 *   list of  predefined options
 */

public class AutocompleteWidget extends  SingleOptionWidget{

    private Option initialOption;
    private Class optionClass;

    public AutocompleteWidget(Class optionClass) {
        this.optionClass = optionClass;
    }

    /**
     *
     * @param  context
     * @return generated html as a string
     * @should accept options with special characters é,ã,ê,ù etc.
     * @should accept options with single or double quotes in middle
     * @should correctly set previous value if initial option is present
     */
    @Override
    public String generateHtml(FormEntryContext context) {

         String optionNames = null;
         String optionValues = null;

         if (context.getMode() == FormEntryContext.Mode.VIEW) {
            String toPrint = "";
            if (getInitialValue() != null) {
                // lookup the label for the selected value
                boolean found = false;
                for (Option o : getOptions()) {
                    if (getInitialValue().equals(o.getLabel())) {
                        toPrint = o.getLabel();
                        found = true;
                        break;
                    }
                }
                if(!found){
                    toPrint = getInitialValue();
                }
                return WidgetFactory.displayValue(toPrint);
            } else {
                return WidgetFactory.displayDefaultEmptyValue();
            }
        }else {
            StringBuilder sb = new StringBuilder();
            String id = context.getFieldName(this);

            if(!getOptions().isEmpty()){
            optionNames = getNamesAsString(getOptions());
            optionValues = getValuesAsString(getOptions());
            }

            // set the previously given option into widget, when editing the form
            if (context.getMode() == FormEntryContext.Mode.EDIT) {
             for (Option o : getOptions()) {
                    if (getInitialValue() != null && getInitialValue().equals(o.getLabel())) {
                        initialOption = new Option(o.getLabel(),o.getValue(),false);
                    }
                }
            }
            // set the default option into widget, when entering the form

            else if (context.getMode() == FormEntryContext.Mode.ENTER) {
               for (Option o : getOptions()) {
                   if (o.isSelected() && !o.getValue().equals("")) {
                      initialOption = new Option(o.getLabel(),o.getValue(),false);
                   }
               }
            }


            sb.append("<input type=\"text\" id=\"" + id + "\" value=\""
			        + ((initialOption != null) ? HtmlUtils.htmlEscape(initialOption.getLabel()) : "")
			        + "\" onblur=\"onBlurAutocomplete(this)\" class=\"optionAutoComplete\""
                    + " onfocus=\"setupOptionAutocomplete(this,'" +optionNames +"','"
                    + optionValues + "')\" onchange=\"setValWhenAutocompleteFieldBlanked(this)\" placeholder=\""
			        + Context.getMessageSourceService().getMessage("htmlformentry.form.value.placeholder") + "\" />");

			sb.append("\n<input type=\"hidden\" class=\"optionAutoCompleteHidden\" id=\"" + id + "_hid" + "\" name=\""
			        + id + "\" value=\"" + ((initialOption != null) ? initialOption.getValue() : "")
			        + "\" />");
            return sb.toString();
        }
    }

    /**
     *
     * @param  options
     * @return a single string with all the option values appended into it
     */
    private String getValuesAsString(List<Option> options) {

       StringBuilder valueSb = new StringBuilder();
            for (Iterator<Option> it = options.iterator(); it.hasNext();) {
                valueSb.append(it.next().getValue());
				if (it.hasNext()){
                   valueSb.append(",");
                }
			}
       return valueSb.toString();
    }

    /*
     * @param  options
     * @return a single string with all the option names appended into it
     */
    private String getNamesAsString(List<Option> options) {

            StringBuilder nameSb = new StringBuilder();
            for (Iterator<Option> it = options.iterator(); it.hasNext();) {
                String originalOption = it.next().getLabel();

                // this is added to eliminate the errors occur due to having ", ' and \ charaters included in
                // the option names. When those are met they are replaced with 'escape character+original character'
                originalOption = originalOption.replace("\\", "\\" +"\\" );
                originalOption = originalOption.replace("'", "\\" +"'");
                originalOption = originalOption.replace("\"", "\\" +"'" );

				nameSb.append(originalOption);
				if (it.hasNext()){
                   nameSb.append(",");
                }
			}
        return nameSb.toString();
    }

    public Option getInitialOption() {
        return initialOption;
    }

    public Class getOptionClass() {
        return optionClass;
    }
}

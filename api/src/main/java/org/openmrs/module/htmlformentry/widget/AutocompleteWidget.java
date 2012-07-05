package org.openmrs.module.htmlformentry.widget;


import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.springframework.web.util.HtmlUtils;

import java.lang.String;
import java.util.Iterator;
import java.util.List;

/**
 *   A single option auto complete widget which provides auto complete suggestions using a list of  predefined options
 */

public class AutocompleteWidget extends  SingleOptionWidget{

    private Option initialOption;
    private String optionNames;
    private String optionValues;

    public AutocompleteWidget() {
    }

    /**
     *
     * @param context
     * @return generated html as a string
     * @should accept options with special characters é,ã,ê,ù etc.
     * @should accept options with single or double quotes in middle
     * @should correctly set previous value if initial option is present
     */
    @Override
    public String generateHtml(FormEntryContext context) {

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
                if (!found){
                    toPrint = getInitialValue();
                }
                return WidgetFactory.displayValue(toPrint);
            } else {
                toPrint = "____";
                return WidgetFactory.displayEmptyValue(toPrint);
            }
        }else {
            StringBuilder sb = new StringBuilder();
            String id = context.getFieldName(this);

            if(!getOptions().isEmpty()){
            this.optionNames = getNamesAsString(getOptions());
            this.optionValues = getValuesAsString(getOptions());
            }

            // set the previously given option into widget, when editing the form, else initialOption is null
            if (context.getMode() == FormEntryContext.Mode.EDIT) {
             for (Option o : getOptions()) {
                    if (getInitialValue() != null && getInitialValue().equals(o.getLabel())) {
                        initialOption = new Option(o.getLabel(),o.getValue(),false);
                    }
                }
            }

            sb.append("<input type=\"text\" id=\"" + id + "\" value=\""
			        + ((initialOption != null) ? HtmlUtils.htmlEscape(initialOption.getLabel()) : "")
			        + "\" onblur=\"onblurOptionAutocomplete(this,'" +this.optionNames +"','" + this.optionValues
                    + "')\" onfocus=\"setupOptionAutocomplete(this,'" +this.optionNames +"','"
                    + this.optionValues + "')\" placeholder=\""
			        + Context.getMessageSourceService().getMessage("htmlformentry.form.value.placeholder") + "\" />");

			sb.append("\n<input type=\"hidden\" id=\"" + id + "_hid" + "\" name=\""
			        + id + "\" value=\"" + ((initialOption != null) ? initialOption.getValue() : "")
			        + "\" />");
            return sb.toString();
        }
    }

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

    private String getNamesAsString(List<Option> options) {

            StringBuilder nameSb = new StringBuilder();
            for (Iterator<Option> it = options.iterator(); it.hasNext();) {
                String originaloption = it.next().getLabel();

                // this is added to eliminate the errors occur due to having ", ' and \ charaters included in
                // the option names. When those are met they are replaced with 'escape character+original character'
                originaloption = originaloption.replace("\\", "\\" +"\\" );
                originaloption = originaloption.replace("'", "\\" +"'");
                originaloption = originaloption.replace("\"", "\\" +"'" );

				nameSb.append(originaloption);
				if (it.hasNext()){
                   nameSb.append(",");
                }
			}
        return nameSb.toString();
    }

    public Option getInitialOption() {
        return initialOption;
    }

    public void setInitialOption(Option initialOption) {
        this.initialOption = initialOption;
    }

    public String getOptionNames() {
        return optionNames;
    }

    public void setOptionNames(String optionNames) {
        this.optionNames = optionNames;
    }

    public String getOptionValues() {
        return optionValues;
    }

    public void setOptionValues(String optionValues) {
        this.optionValues = optionValues;
    }

}

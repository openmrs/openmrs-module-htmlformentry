package org.openmrs.module.htmlformentry.widget;

import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.JavaScriptUtils;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: isha
 * Date: 5/25/12
 * Time: 10:10 PM
 * To change this template use File | Settings | File Templates.
 */

public class AutocompleteWidget extends  SingleOptionWidget{

    private Option initialOption;

    public AutocompleteWidget() {
    }


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
             for (Option o : getOptions()) {
                    if (getInitialValue().equals(o.getLabel())) {
                        initialOption = new Option(o.getLabel(),o.getValue(),false);
                    }
                }

            sb.append("<input type=\"text\" id=\"display_" + context.getFieldName(this) + "\" value=\""
			        + ((initialOption != null) ? HtmlUtils.htmlEscape(initialOption.getLabel()) : "")
			        + "\" onblur=\"updateFields(this)\" placeholder=\""
			        + Context.getMessageSourceService().getMessage("htmlformentry.form.value.placeholder") + "\" />");
			sb.append("\n<input type=\"hidden\" id=\"" + context.getFieldName(this) + "\" name=\""
			        + context.getFieldName(this) + "\" value=\"" + ((initialOption != null) ? initialOption.getValue() : "")
			        + "\" />");
			sb.append("\n<script>");
			sb.append("\nvar optionLabelValueMap = new Object();");
			ArrayList<String> escapedOptionNames = new ArrayList<String>(getOptions().size());
			for (Option option : getOptions()) {
				String escapeOptionName = JavaScriptUtils.javaScriptEscape(option.getLabel());
				escapedOptionNames.add(escapeOptionName);
				sb.append("\noptionLabelValueMap[\"" + escapeOptionName + "\"] = " + option.getValue() + ";");
			}
			sb.append("\n");
			//clear the form field when user clears the field or if no valid selection is made
			sb.append("\nfunction updateFields(displayField){");
			sb.append("\n	if(optionLabelValueMap[$j.trim($j(displayField).val())] == undefined)");
			sb.append("\n		$j(displayField).val('');");
			sb.append("\n	if($j.trim($j(displayField).val()) == '')");
			sb.append("\n		$j(\"#" + id + "\").val('');");
			sb.append("\n}");
			sb.append("\n");
			sb.append("\n$j('input#display_" + id + "').autocomplete({");
			sb.append("\n	source:[" + StringUtils.collectionToDelimitedString(escapedOptionNames, ",", "\"", "\"") + "],");
			sb.append("\n	select: function(event, ui) {");
			sb.append("\n				$j(\"#" + id + "\").val(optionLabelValueMap[ui.item.value]);");
			sb.append("\n			}");
			sb.append("\n});");
			sb.append("</script>");



            return sb.toString();
        }
    }
}

package org.openmrs.module.htmlformentry.widget;

import org.openmrs.Concept;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.JavaScriptUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: isha
 * Date: 5/25/12
 * Time: 10:10 PM
 * To change this template use File | Settings | File Templates.
 */

public class AutocompleteWidget extends  SingleOptionWidget{

    private Option option;

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
                    if (getInitialValue().equals(o.getValue())) {
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

            sb.append("<input type=\"text\" id=\"display_" + context.getFieldName(this) + "\" value=\""
			        + ((option != null) ? HtmlUtils.htmlEscape(option.getLabel()) : "")
			        + "\" onblur=\"updateLocationFields(this)\" placeholder=\""
			        + Context.getMessageSourceService().getMessage("htmlformentry.form.value.placeholder") + "\" />");
			sb.append("\n<input type=\"hidden\" id=\"" + context.getFieldName(this) + "\" name=\""
			        + context.getFieldName(this) + "\" value=\"" + ((option != null) ? option.getValue() : "")
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
			sb.append("\nfunction updateLocationFields(displayField){");
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

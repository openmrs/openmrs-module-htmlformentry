package org.openmrs.module.htmlformentry.widget;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.PersonName;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.springframework.util.StringUtils;

/**
 * A widget that allows the input of a Person name. Implemented using text fields
 * that accept a Family Name and a Given Name.
 */
public class PersonNameWidget implements Widget {
	
	private PersonName initialValue;
	
	public PersonNameWidget() {
	}
	
	@Override
    public String generateHtml(FormEntryContext context) {
		StringBuilder sb = new StringBuilder();
        if (context.getMode() == Mode.VIEW) {
            String toPrint = "";
            if (initialValue != null) {
                toPrint = initialValue.toString();
                return WidgetFactory.displayValue(toPrint);
            } else {
                return WidgetFactory.displayDefaultEmptyValue();
            }
        } else {
            String id = context.getFieldName(this);
            sb.append("Family name: ");
            sb.append("<input type=\"text\" id=\"" + id + "_family\" name=\"" + id + "_family\"");
            // TODO escape value
            if (initialValue != null)
            	sb.append(" value=\"" + initialValue.getFamilyName() + "\"");
            sb.append("/>");
            
            sb.append("Given name: ");
            sb.append("<input type=\"text\" id=\"" + id + "_given\" name=\"" + id + "_given\"");
            // TODO escape value
            if (initialValue != null)
            	sb.append(" value=\"" + initialValue.getGivenName() + "\"");
            sb.append("/>");
        }
        return sb.toString();
	}
	
	@Override
    public PersonName getValue(FormEntryContext context, HttpServletRequest request) {
		String family = (String) HtmlFormEntryUtil.getParameterAsType(request, context.getFieldName(this) + "_family", String.class);
		String given = (String) HtmlFormEntryUtil.getParameterAsType(request, context.getFieldName(this) + "_give", String.class);
		if (StringUtils.hasText(family) || StringUtils.hasText(given)) {
			return new PersonName(given, null, family);
		} else {
			return null;
		}
	}
	
	@Override
    public void setInitialValue(Object initialValue) {
		this.initialValue = (PersonName) initialValue;
	}
	
}

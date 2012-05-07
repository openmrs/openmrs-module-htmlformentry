package org.openmrs.module.htmlformentry.widget;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;

/**
 * A widget that implements a hidden input field like {@code <input
 * type="hidden"/>},
 */
public class HiddenFieldWidget implements Widget {

	private String initialValue;
	/**
	 * Gets the initial value associated with this widget
	 * 
	 * @return the initialValue
	 */
	public String getInitialValue() {
		return initialValue;
	}

	/**
	 * @param initialValue
	 *            the initialValue to set
	 */
	@Override
    public void setInitialValue(Object initialValue) {
		this.initialValue = (String) initialValue;
	}

	@Override
    public String generateHtml(FormEntryContext context) {
		StringBuilder sb = new StringBuilder();
		if (context.getMode() != Mode.VIEW) {
			sb.append("<input type=\"hidden\" name=\"" + context.getFieldName(this) + "\" id=\"" + context.getFieldName(this) + "\"");
			if (initialValue != null)
				sb.append(" value=\"" + initialValue + "\"");
			sb.append("/>");
		}
		return sb.toString();
	}

	@Override
    public Object getValue(FormEntryContext context, HttpServletRequest request) {
		return request.getParameter(context.getFieldName(this));
	}

}

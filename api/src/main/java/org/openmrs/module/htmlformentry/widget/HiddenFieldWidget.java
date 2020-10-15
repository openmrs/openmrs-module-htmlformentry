package org.openmrs.module.htmlformentry.widget;

import javax.servlet.http.HttpServletRequest;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;

/**
 * A widget that implements a hidden input field like {@code <input type="hidden"/>},
 */
public class HiddenFieldWidget implements Widget {
	
	private String initialValue;
	
	private String label;
	
	private Map<String, String> attributes;
	
	/**
	 * Gets the initial value associated with this widget
	 * 
	 * @return the initialValue
	 */
	public String getInitialValue() {
		return initialValue;
	}
	
	/**
	 * @param initialValue the initialValue to set
	 */
	@Override
	public void setInitialValue(Object initialValue) {
		this.initialValue = (String) initialValue;
	}
	
	@Override
	public String generateHtml(FormEntryContext context) {
		StringBuilder sb = new StringBuilder();
		if (context.getMode() != Mode.VIEW) {
			sb.append("<input type=\"hidden\" name=\"").append(context.getFieldName(this)).append("\"");
			sb.append(" id=\"").append(context.getFieldName(this)).append("\"");
			for (String att : getAttributes().keySet()) {
				sb.append(" ").append(att).append("=\"").append(getAttributes().get(att)).append("\"");
			}
			if (initialValue != null) {
				sb.append(" value=\"").append(initialValue).append("\"");
			}
			sb.append("/>");
		}
		if (StringUtils.isNotBlank(label)) {
			sb.append(label);
		}
		return sb.toString();
	}
	
	@Override
	public Object getValue(FormEntryContext context, HttpServletRequest request) {
		return request.getParameter(context.getFieldName(this));
	}
	
	public Map<String, String> getAttributes() {
		if (attributes == null) {
			attributes = new LinkedHashMap<>();
		}
		return attributes;
	}
	
	public void addAttribute(String key, String value) {
		getAttributes().put(key, value);
	}
	
	public void setLabel(String label) {
		this.label = label;
	}
}

package org.openmrs.module.htmlformentry.widget;

import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;

import javax.servlet.http.HttpServletRequest;

/**
 * A checkbox widget, like {@code <input type="checkbox"/>}
 */
public class CheckboxWidget implements Widget {

    private Object initialValue;
    private String value = "true";
    private String label;
    private boolean toggleDimInd = false;
    private String toggleTarget;

    public CheckboxWidget() { }
    
    public CheckboxWidget(String value) {
        this.value = value;
    }

    public CheckboxWidget(String label, String value) {
        this.label = label;
        this.value = value;
    }

    public CheckboxWidget(String label, String value, String toggleTarget) {
        this.label = label;
        this.value = value;
        this.toggleTarget = toggleTarget;
    }
    
    public CheckboxWidget(String label, String value, String toggleTarget, boolean toggleDimInd) {
        this.label = label;
        this.value = value;
        this.toggleTarget = toggleTarget;
        this.toggleDimInd = toggleDimInd;
    }
    /**
     * Gets the value attribute for the checkbox. Not to be confused with {@see getValue(FormEntryContext,HttpServletRequest)}.
     * 
     * @return value of the widget
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value attribute for the checkbox.
     * 
     * @param value
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Gets the text label to display before the checkbox.
     * 
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the text label to display after the checkbox.
     * 
     * @param label
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * @see org.openmrs.module.htmlformentry.widget.Widget#generateHtml(org.openmrs.module.htmlformentry.FormEntryContext)
     */
    @Override
    public String generateHtml(FormEntryContext context) {
        StringBuilder sb = new StringBuilder();
        if (context.getMode() == Mode.VIEW) {
            String labelString = "";
            if  (label != null)
                labelString = "&#160;" + label;
            if (initialValue != null) {
                sb.append(WidgetFactory.displayValue("[X]" + labelString));
            } else {
                sb.append(WidgetFactory.displayEmptyValue("[&#160;&#160;]" + labelString));
            }    
        } else {
            sb.append("<input type=\"checkbox\" id=\"").append(context.getFieldName(this)).append("\" name=\"").append(context.getFieldName(this))
                .append("\" value=\"").append(value).append("\"");
            if (initialValue != null && !"".equals(initialValue))
                sb.append(" checked=\"true\"");
            if (toggleTarget != null && toggleTarget.trim().length() > 0) 
            	sb.append(" toggle" + (toggleDimInd ? "Dim" : "Hide") + "=\"" + toggleTarget + "\"");
            sb.append("/>");
            if (label != null)
                sb.append("<label for=\"").append(context.getFieldName(this)).append("\">").append(label).append("</label>");
            sb.append("<input type=\"hidden\" name=\"_").append(context.getFieldName(this)).append("\"/>");
        }
        return sb.toString();
    }

    /**
     * @see org.openmrs.module.htmlformentry.widget.Widget#getValue(org.openmrs.module.htmlformentry.FormEntryContext, javax.servlet.http.HttpServletRequest)
     */
    @Override
    public Object getValue(FormEntryContext context, HttpServletRequest request) {
        return request.getParameter(context.getFieldName(this));
    }

    /**
     * @see org.openmrs.module.htmlformentry.widget.Widget#setInitialValue(java.lang.Object)
     */
    @Override
    public void setInitialValue(Object initialValue) {
        this.initialValue = initialValue;
    }

	public String getToggleTarget() {
		return toggleTarget;
	}

	public void setToggleTarget(String toggleTarget) {
		this.toggleTarget = toggleTarget;
	}

	public boolean isToggleDimInd() {
		return toggleDimInd;
	}

	public void setToggleDimInd(boolean toggleDimInd) {
		this.toggleDimInd = toggleDimInd;
	}

}

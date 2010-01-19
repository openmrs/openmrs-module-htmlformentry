package org.openmrs.module.htmlformentry.widget;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;

/**
 * Like <input type="checkbox"/>
 */
public class CheckboxWidget implements Widget {

    private Object initialValue;
    private String value = "true";
    private String label;

    public CheckboxWidget() { }
    
    public CheckboxWidget(String value) {
        this.value = value;
    }

    public CheckboxWidget(String label, String value) {
        this.label = label;
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * @see org.openmrs.module.htmlformentry.widget.Widget#generateHtml(org.openmrs.module.htmlformentry.FormEntryContext)
     */
    public String generateHtml(FormEntryContext context) {
        StringBuilder sb = new StringBuilder();
        if (context.getMode() == Mode.VIEW) {
            String labelString = "";
            if  (label != null)
                labelString = "&nbsp;" + label;
            if (initialValue != null) {
                sb.append(WidgetFactory.displayValue("[X]" + labelString));
            } else {
                sb.append(WidgetFactory.displayEmptyValue("[&nbsp;&nbsp;]" + labelString));
            }    
        } else {
            sb.append("<input type=\"hidden\" name=\"_").append(context.getFieldName(this)).append("\"/>");
            sb.append("<input type=\"checkbox\" name=\"").append(context.getFieldName(this)).append("\" value=\"").append(
                    value).append("\"");
            if (initialValue != null && !"".equals(initialValue))
                sb.append(" checked=\"true\"");
            sb.append("/>");
            if (label != null)
                sb.append(label);
        }
        return sb.toString();
    }

    /**
     * @see org.openmrs.module.htmlformentry.widget.Widget#getValue(org.openmrs.module.htmlformentry.FormEntryContext, javax.servlet.http.HttpServletRequest)
     */
    public Object getValue(FormEntryContext context, HttpServletRequest request) {
        return request.getParameter(context.getFieldName(this));
    }

    /**
     * @see org.openmrs.module.htmlformentry.widget.Widget#setInitialValue(java.lang.Object)
     */
    public void setInitialValue(Object initialValue) {
        this.initialValue = value;
    }

}

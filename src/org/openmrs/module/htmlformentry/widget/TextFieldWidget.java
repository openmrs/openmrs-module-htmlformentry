package org.openmrs.module.htmlformentry.widget;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;

public class TextFieldWidget implements Widget {
    
    private Boolean textArea = false;
    private Integer textFieldSize;
    private Integer textAreaRows;
    private Integer textAreaColumns;
    private String initialValue;
    
    public TextFieldWidget() {
        this(false);
    }
    
    public TextFieldWidget(Boolean textArea) {
        this.textArea = textArea;
    }
    
    public TextFieldWidget(Integer size) {
        this(false);
        textFieldSize = size;
    }
    
    public TextFieldWidget(Integer rows, Integer columns) {
        this(true);
        textAreaRows = rows;
        textAreaColumns = columns;
    }

    /**
     * @return the initialValue
     */
    public String getInitialValue() {
        return initialValue;
    }

    /**
     * @param initialValue the initialValue to set
     */
    public void setInitialValue(Object initialValue) {
        this.initialValue = (String) initialValue;
    }

    public String generateHtml(FormEntryContext context) {
        StringBuilder sb = new StringBuilder();
        if (context.getMode().equals(Mode.VIEW)) {
            String toPrint = "";
            if (initialValue != null) {
                toPrint = initialValue.toString();
                return WidgetFactory.displayValue(toPrint);
            } else {
                if (textAreaRows != null) {
                    toPrint = "";
                    for (int i = 0; i < textAreaRows; i += 2)
                        toPrint += "\n";
                    return WidgetFactory.displayValue(toPrint);
                } else {
                    toPrint = "_______________";
                    return WidgetFactory.displayEmptyValue(toPrint);
                }
            }
        } else {
            if (textArea) {
                sb.append("<textarea name=\"" + context.getFieldName(this) + "\" id=\"" + context.getFieldName(this) + "\"");
                if (textAreaRows != null)
                    sb.append(" rows=\"" + textAreaRows + "\"");
                if (textAreaColumns != null)
                    sb.append(" cols=\"" + textAreaColumns + "\"");
                sb.append(">");
                if (initialValue != null)
                    sb.append(initialValue);
                sb.append("</textarea>");
            } else {
                sb.append("<input type=\"text\" name=\"" + context.getFieldName(this) + "\" id=\"" + context.getFieldName(this) + "\"");
                if (textFieldSize != null)
                    sb.append(" size=\"" + textFieldSize + "\"");
                if (initialValue != null)
                    sb.append(" value=\"" + initialValue + "\"");
                sb.append("/>");
            }
        }
        return sb.toString();
    }

    public Object getValue(FormEntryContext context, HttpServletRequest request) {
        return request.getParameter(context.getFieldName(this));
    }

}

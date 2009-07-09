package org.openmrs.module.htmlformentry.widget;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.module.htmlformentry.FormEntryContext;

public class ErrorWidget implements Widget {

    public String generateHtml(FormEntryContext context) {
        return "<span class=\"error\" id=\"" + context.getFieldName(this) + "\"></span>";
    }

    public Object getValue(FormEntryContext context, HttpServletRequest request) {
        return null;
    }

    public void setInitialValue(Object initialValue) {
        throw new UnsupportedOperationException(); 
    }

}

package org.openmrs.module.htmlformentry.widget;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.module.htmlformentry.FormEntryContext;

/**
 * A widget the represents as error field.
 */
public class ErrorWidget implements Widget {

    public String generateHtml(FormEntryContext context) {
    	// error widgets are always hidden at first--they are revealed by javascript if necessary  
        return "<span class=\"error\" style=\"display: none\" id=\"" + context.getFieldName(this) + "\"></span>";
    }

    public Object getValue(FormEntryContext context, HttpServletRequest request) {
        return null;
    }

    public void setInitialValue(Object initialValue) {
        throw new UnsupportedOperationException(); 
    }

}

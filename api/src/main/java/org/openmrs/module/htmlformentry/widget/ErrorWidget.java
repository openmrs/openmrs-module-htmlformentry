package org.openmrs.module.htmlformentry.widget;

import org.openmrs.module.htmlformentry.FormEntryContext;

import javax.servlet.http.HttpServletRequest;

/**
 * A widget the represents as error field.
 */
public class ErrorWidget implements Widget {

    @Override
    public String generateHtml(FormEntryContext context) {
    	// error widgets are always hidden at first--they are revealed by javascript if necessary  
        return "<span class=\"error field-error\" style=\"display: none\" id=\"" + context.getFieldName(this) + "\"></span>";
    }

    @Override
    public Object getValue(FormEntryContext context, HttpServletRequest request) {
        return null;
    }

    @Override
    public void setInitialValue(Object initialValue) {
        throw new UnsupportedOperationException(); 
    }

}

package org.openmrs.module.htmlformentry.widget;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.module.htmlformentry.FormEntryContext;

/**
 * This represents a single widget on a form, such as a checkbox, a dropdown, or a group of radio buttons.
 * It is capable of producing HTML, and later getting the widget's value back from a form submission.  
 */
public interface Widget {

    /**
     * If we are doing a VIEW or EDIT, then the framework will call this to prepopulate the widget
     */
    public void setInitialValue(Object initialValue);

    /**
     * Called by the framework to draw this widget on a page
     * 
     * @return
     */
    public String generateHtml(FormEntryContext context);

    
    /**
     * After the form is submitted, this widget is able to fetch the values out of the input
     * elements it rendered when getHtml was called.
     * 
     * @return
     */
    public Object getValue(FormEntryContext context, HttpServletRequest request);
    
}

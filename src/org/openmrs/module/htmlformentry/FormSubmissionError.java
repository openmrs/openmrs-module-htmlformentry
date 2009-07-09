package org.openmrs.module.htmlformentry;

import org.openmrs.module.htmlformentry.widget.Widget;

/**
 * Encapsulates an error that may be raised while validating a form submission 
 */
public class FormSubmissionError {

    private Widget sourceWidget;
    private String id;
    private String error;
    
    public FormSubmissionError(Widget sourceWidget, String error) {
        this.sourceWidget = sourceWidget;
        this.error = error;
    }
    
    public FormSubmissionError(String id, String error) {
        this.id = id;
        this.error = error;
    }

    public Widget getSourceWidget() {
        return sourceWidget;
    }

    public void setSourceWidget(Widget sourceWidget) {
        this.sourceWidget = sourceWidget;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
    
}

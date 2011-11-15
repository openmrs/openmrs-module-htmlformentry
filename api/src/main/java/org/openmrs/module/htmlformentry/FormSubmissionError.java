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
    
    @Override
    public String toString() {
    	return (sourceWidget != null ? sourceWidget.getClass().getSimpleName() : id) + " -> " + error;
    }

    /**
     * Gets the widget that is the source of the error.
     * 
     * @return the widget that is the source of the error
     */
    public Widget getSourceWidget() {
        return sourceWidget;
    }

    /**
     * Sets the widget that is the source of the error.
     * 
     * @param sourceWidget the widget that is the source of the error
     */
    public void setSourceWidget(Widget sourceWidget) {
        this.sourceWidget = sourceWidget;
    }

    /**
     * Gets the ID
     * 
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the ID
     * 
     * @param id value to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the error message
     * 
     * @return the error message
     */
    public String getError() {
        return error;
    }

    /**
     * Sets the error message
     * 
     * @param error the error message to set
     */
    public void setError(String error) {
        this.error = error;
    }
    
}

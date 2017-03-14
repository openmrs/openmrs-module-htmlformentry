package org.openmrs.module.htmlformentry.widget;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.OpenmrsObject;
import org.openmrs.module.htmlformentry.FormEntryContext;

/**
 * This represents a single widget on a form which presents several coded options, of which only one 
 * may be selected, such as a dropdown, or a group of radio buttons. 
 */
public abstract class SingleOptionWidget implements Widget {
	
	private String initialValue;
	private List<Option> options;
	
	/**
	 * Default Constructor
	 */
	public SingleOptionWidget() { }
	
    /**
     * @see Widget#setInitialValue(java.lang.Object)
     */
    @Override
    public void setInitialValue(Object initialValue) {
        if (initialValue == null)
            this.initialValue = null;
		else {
			if (initialValue instanceof OpenmrsObject) {
				this.initialValue = ((OpenmrsObject) initialValue).getId().toString();
			}
			else {
				this.initialValue = initialValue.toString();
			}
		}
    }
    
    /**
     * Returns the initial value set on this Widget
     * @return
     */
    public String getInitialValue() {
    	return initialValue;
    }
    
    /**
     * @see Widget#getValue(FormEntryContext, HttpServletRequest)
     */
    @Override
    public Object getValue(FormEntryContext context, HttpServletRequest request) {
        return request.getParameter(context.getFieldName(this));
    }

	/**
	 * Adds an Option to this Widget
	 * @param option
	 */
    public void addOption(Option option) {
        if (options == null)
            options = new ArrayList<Option>();
        options.add(option);
    }    

    /**
     * Returns all Options for this Widget
     * @return
     */
    public List<Option> getOptions() {
        return options;
    }

    /**
     * Sets all Options for this Widget
     * @param options
     */
    public void setOptions(List<Option> options) {
        this.options = options;
    }
}

package org.openmrs.module.htmlformentry.widget;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

import org.openmrs.Concept;
import org.openmrs.OpenmrsData;
import org.openmrs.OpenmrsMetadata;
import org.openmrs.OpenmrsObject;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.element.ValueStub;
import org.openmrs.util.OpenmrsUtil;

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
	public SingleOptionWidget() {
	}
	
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
			} else {
				this.initialValue = initialValue.toString();
			}
			addOptionIfMissing(initialValue);
		}
	}
	
	/**
	 * The purpose of this method is to determine if the initial value configured on the widget is among
	 * the options configured for the widget. If it is not among the options, then add it to the
	 * available options, in order to ensure that existing forms that are opened and no longer contain
	 * the same options that they were saved with are able to properly display and edit existing data
	 * without loss.
	 */
	protected void addOptionIfMissing(Object initialValue) {
		Option initialValueOption = convertToOption(initialValue);
		if (initialValueOption != null) {
			addOption(initialValueOption);
		}
	}
	
	/**
	 * This method converts a generic initial value into an Option. The intention is to support all of
	 * the current use cases that exist in the module (all of the data types that would be passed into
	 * this method for various usages).
	 */
	protected Option convertToOption(Object initialValue) {
		if (initialValue == null) {
			return null;
		}
		if (initialValue instanceof OpenmrsMetadata) {
			OpenmrsMetadata val = (OpenmrsMetadata) initialValue;
			return new Option(HtmlFormEntryUtil.format(val), val.getId().toString(), true);
		} else if (initialValue instanceof Concept) {
			Concept val = (Concept) initialValue;
			return new Option(val.getDisplayString(), val.getId().toString(), true);
		} else if (initialValue instanceof ValueStub) {
			ValueStub val = (ValueStub) initialValue;
			return new Option(val.getDisplayValue(), val.getId().toString(), true);
		} else if (initialValue instanceof OpenmrsData) {
			OpenmrsData val = (OpenmrsData) initialValue;
			return new Option(val.toString(), val.getId().toString(), true);
		} else {
			return new Option(initialValue.toString(), initialValue.toString(), true);
		}
	}
	
	/**
	 * Returns the initial value set on this Widget
	 * 
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
	 * 
	 * @param option
	 */
	public void addOption(Option option) {
		if (options == null) {
			options = new ArrayList<>();
		}
		boolean optionFound = false;
		for (Option existingOption : options) {
			if (OpenmrsUtil.nullSafeEquals(option.getValue(), existingOption.getValue())) {
				optionFound = true;
			}
		}
		if (!optionFound) {
			options.add(option);
		}
	}
	
	/**
	 * Returns all Options for this Widget
	 * 
	 * @return
	 */
	public List<Option> getOptions() {
		return options;
	}
	
	/**
	 * Sets all Options for this Widget
	 * 
	 * @param options
	 */
	public void setOptions(List<Option> options) {
		this.options = options;
		addOptionIfMissing(initialValue);
	}
	
}

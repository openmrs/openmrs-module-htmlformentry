package org.openmrs.module.htmlformentry.widget;

import org.openmrs.module.htmlformentry.FormEntryContext;


/**
 *Gadget is a  collection of one or more  widgets. This Gadget is used for Custom complex fields (like Name, Address).
 *
 */
public abstract class Gadget implements Widget {
	/**
	 * To check if the widgets in the gadgets are already registered
	 */
	protected boolean isRegistered;
	
	/**
	 * To Register all the widgets in the gadgets
	 */
	protected abstract void registerWidgets(FormEntryContext context);
}

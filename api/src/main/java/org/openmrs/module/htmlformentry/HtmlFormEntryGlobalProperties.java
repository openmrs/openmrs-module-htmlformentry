package org.openmrs.module.htmlformentry;

import org.openmrs.api.context.Context;


public class HtmlFormEntryGlobalProperties {
	
	/**
	 * @return global property boolean that specifies whether or not to void encounter by html form schema
	 * returns null if property is null or is set to neither true or false
	 */
	public static final Boolean VOID_ENCOUNTER_BY_HTML_FORM_SCHEMA () {
		String propertyValue = Context.getAdministrationService().getGlobalProperty("htmlformentry.voidEncounterByHtmlFormSchema");
		if ("true".equalsIgnoreCase(propertyValue)) {
			return true;
		}
		else if ("false".equalsIgnoreCase(propertyValue)) {
			return false;
		}
		else {
			return null;
		}
	}
	

	/**
	 * @return boolean describing whether or not the html form flowsheet module has been started 
	 */
	public static final Boolean HTML_FORM_FLOWSHEET_STARTED () {
		String propertyValue = Context.getAdministrationService().getGlobalProperty("htmlformflowsheet.started");
		if ("true".equalsIgnoreCase(propertyValue)) {
			return true;
		}
		else {
			return false;
		}
	} 	
}

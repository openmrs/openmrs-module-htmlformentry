package org.openmrs.module.htmlformentry.web;

import org.openmrs.module.htmlformentry.HtmlFormEntryWebAppContext;
import org.openmrs.web.WebConstants;

/**
 * Copies the actual deployed webapp context name into {@link HtmlFormEntryWebAppContext}, so that
 * api-tier code can build URLs without depending on the openmrs-web module. Wired up as a bean in
 * webModuleApplicationContext.xml, which is only loaded in a real web deployment.
 */
public class WebAppContextInitializer {

	public void init() {
		HtmlFormEntryWebAppContext.setWebAppName(WebConstants.WEBAPP_NAME);
	}
}

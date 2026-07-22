package org.openmrs.module.htmlformentry;

/**
 * Holds the name of the webapp context that OpenMRS is deployed under (equivalent to
 * org.openmrs.web.WebConstants#WEBAPP_NAME), so that api-tier code that needs to build absolute
 * URLs doesn't need to depend on the openmrs-web module. The default matches WebConstants'
 * default; the omod module overwrites it at startup with the actual deployed value.
 */
public class HtmlFormEntryWebAppContext {

	private static String webAppName = "openmrs";

	public static String getWebAppName() {
		return webAppName;
	}

	public static void setWebAppName(String webAppName) {
		HtmlFormEntryWebAppContext.webAppName = webAppName;
	}
}

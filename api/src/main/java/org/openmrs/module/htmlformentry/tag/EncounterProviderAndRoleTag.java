/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.htmlformentry.tag;

import java.util.List;
import java.util.Map;

import org.openmrs.EncounterRole;
import org.openmrs.Provider;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.util.MatchMode;
import org.openmrs.module.htmlformentry.widget.ProviderAjaxAutoCompleteWidget;
import org.openmrs.module.htmlformentry.widget.ProviderWidget;
import org.openmrs.module.htmlformentry.widget.Widget;

/**
 * Defines the configuration attributes available in the encounterProviderAndRole tag, and provides
 * parsing and validation
 */
public class EncounterProviderAndRoleTag {
	
	private boolean required; // Whether or not this is required or not
	
	private int count; // The number of provider widgets to render
	
	private String providerWidgetSeparator; // If count > 1, the html that should separate the widgets
	
	private EncounterRole encounterRole; // Whether this should be used specifically to set a specific Encounter Role
	
	private boolean autocompleteProvider; // Whether autocomplete or dropdown (default is dropdown)
	
	private List<String> providerRoles; // Comma-separated list of roles to limit providers to
	
	private MatchMode providerMatchMode; // For autocomplete, what match mode to use for searching
	
	private Provider defaultValue; // Can set to "currentuser" or a specific provider id/uuid
	
	private List<Provider> providers; // Enables population with list of allowed providers
	
	private boolean includeRetired; //Doesnot display retired providers if set to false
	
	/**
	 * @param parameters
	 */
	public EncounterProviderAndRoleTag(Map<String, String> parameters) {
		required = TagUtil.parseParameter(parameters, "required", Boolean.class, false);
		count = TagUtil.parseParameter(parameters, "count", Integer.class, 1);
		providerWidgetSeparator = TagUtil.parseParameter(parameters, "providerWidgetSeparator", String.class, ", ");
		encounterRole = TagUtil.parseParameter(parameters, "encounterRole", EncounterRole.class);
		autocompleteProvider = TagUtil.parseParameter(parameters, "autocompleteProvider", Boolean.class, false);
		includeRetired = TagUtil.parseParameter(parameters, "includeRetired", Boolean.class, false);
		providerMatchMode = TagUtil.parseParameter(parameters, "providerMatchMode", MatchMode.class, MatchMode.ANYWHERE);
		defaultValue = TagUtil.parseParameter(parameters, "default", Provider.class);
		providerRoles = TagUtil.parseListParameter(parameters, "providerRoles", String.class);
		if (!autocompleteProvider) {
			providers = HtmlFormEntryUtil.getProviders(providerRoles, true, includeRetired);
		}
	}
	
	public Widget instantiateProviderWidget() {
		if (isAutocompleteProvider()) {
			return new ProviderAjaxAutoCompleteWidget(getProviderMatchMode(), providerRoles);
		} else {
			return new ProviderWidget(providers);
		}
	}
	
	public boolean isRequired() {
		return required;
	}
	
	public int getCount() {
		return count;
	}
	
	public String getProviderWidgetSeparator() {
		return providerWidgetSeparator;
	}
	
	public EncounterRole getEncounterRole() {
		return encounterRole;
	}
	
	public boolean isAutocompleteProvider() {
		return autocompleteProvider;
	}
	
	public MatchMode getProviderMatchMode() {
		return providerMatchMode;
	}
	
	public Provider getDefaultValue() {
		return defaultValue;
	}
	
	public List<String> getProviderRoles() {
		return providerRoles;
	}
	
	public List<Provider> getProviders() {
		return providers;
	}
}

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
package org.openmrs.module.htmlformentry;

import java.util.Set;

import org.openmrs.OpenmrsObject;
import org.openmrs.annotation.OpenmrsProfile;
import org.openmrs.module.htmlformentry.compatibility.RegimenSuggestionCompatibility;
import org.openmrs.module.htmlformentry.handler.AttributeDescriptor;
import org.springframework.stereotype.Component;

@Component("htmlformentry.RegimenSuggestionCompatibility")
@OpenmrsProfile(openmrsPlatformVersion = "2.*")
public class RegimenSuggestionCompatibility2_0 implements RegimenSuggestionCompatibility {

	@Override
	public void AddDrugDependencies(String id, AttributeDescriptor attributeDescriptor, Set<OpenmrsObject> dependencies) {
		//this is left empty because RegimenSuggestion was removed in platform 2.0
	}
}
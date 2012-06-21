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
package org.openmrs.module.htmlformentry.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openmrs.EncounterType;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.element.EncounterDetailSubmissionElement;

/**
 * Handles the {@code <encountertType>} tag
 */
public class EncounterTypeTagHandler extends SubstitutionTagHandler implements TagHandler {
	
	/**
	 * @see org.openmrs.module.htmlformentry.handler.AbstractTagHandler#createAttributeDescriptors()
	 */
	@Override
	protected List<AttributeDescriptor> createAttributeDescriptors() {
		List<AttributeDescriptor> attributeDescriptors = new ArrayList<AttributeDescriptor>();
		attributeDescriptors.add(new AttributeDescriptor("types", EncounterType.class));
		attributeDescriptors.add(new AttributeDescriptor("default", EncounterType.class));
		return Collections.unmodifiableList(attributeDescriptors);
	}
	
	/**
	 * @see org.openmrs.module.htmlformentry.handler.SubstitutionTagHandler#getSubstitution(org.openmrs.module.htmlformentry.FormEntrySession,
	 *      org.openmrs.module.htmlformentry.FormSubmissionController, java.util.Map)
	 */
	@Override
	protected String getSubstitution(FormEntrySession session, FormSubmissionController controllerActions,
	                                 Map<String, String> parameters) {
		Map<String, Object> temp = new HashMap<String, Object>();
		temp.putAll(parameters);
		temp.put("encounterType", true);
		EncounterDetailSubmissionElement element = new EncounterDetailSubmissionElement(session.getContext(), temp);
		session.getSubmissionController().addAction(element);
		
		return element.generateHtml(session.getContext());
	}
	
}

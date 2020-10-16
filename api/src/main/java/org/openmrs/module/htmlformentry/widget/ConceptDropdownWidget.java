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
package org.openmrs.module.htmlformentry.widget;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openmrs.Concept;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.schema.ObsFieldAnswer;

/**
 * Widget that lets you choose a {@link Concept} from a dropdown
 */
public class ConceptDropdownWidget extends DropdownWidget {
	
	private Map<String, Concept> conceptVals = new LinkedHashMap<>();
	
	public ConceptDropdownWidget(List<Concept> optionConcepts, String emptyLabel) {
		super();
		setOptions(new ArrayList<>());
		if (emptyLabel != null) {
			addOption(null, emptyLabel);
		}
		for (Concept concept : optionConcepts) {
			addOption(concept, concept.getDisplayString());
		}
	}

	public ConceptDropdownWidget(List<ObsFieldAnswer> answers) {
		super();
		setOptions(new ArrayList<>());
		if (answers != null) {
			for (ObsFieldAnswer answer : answers) {
				addOption(answer.getConcept(), answer.getDisplayName());
			}
		}
	}

	protected void addOption(Concept c, String label) {
		String id = (c == null ? "" : c.getId().toString());
		getOptions().add(new Option(label, id, false));
		if (c != null) {
			conceptVals.put(id, c);
		}
	}
	
	public void setInitialConceptValue(Concept concept) {
		if (concept == null) {
			setInitialValue(null);
		} else {
			setInitialValue(concept.getId().toString());
		}
	}
	
	public Concept getConceptValue(FormEntryContext context, HttpServletRequest request) {
		Object value = getValue(context, request);
		return value == null ? null : conceptVals.get(value.toString());
	}
}

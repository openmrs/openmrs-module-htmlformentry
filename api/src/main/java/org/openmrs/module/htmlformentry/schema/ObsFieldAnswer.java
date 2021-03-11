package org.openmrs.module.htmlformentry.schema;

import org.openmrs.Concept;

/**
 * Represents an Obs field answer in the HTML Form Schema
 */
public class ObsFieldAnswer extends ConceptOption {
	
	public ObsFieldAnswer() {
		super();
	}
	
	public ObsFieldAnswer(String displayName, Concept concept) {
		super(displayName, concept);
	}
}

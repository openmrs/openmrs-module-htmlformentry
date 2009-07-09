package org.openmrs.module.htmlformentry.schema;

import org.openmrs.Concept;

public class ObsFieldAnswer {

	private String displayName;
	private Concept concept;
    
    public ObsFieldAnswer() { }

	/**
	 * @return the displayName
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * @param displayName the displayName to set
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * @return the concept
	 */
	public Concept getConcept() {
		return concept;
	}

	/**
	 * @param concept the concept to set
	 */
	public void setConcept(Concept concept) {
		this.concept = concept;
	}
}

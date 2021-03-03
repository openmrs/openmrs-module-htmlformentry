package org.openmrs.module.htmlformentry.schema;

import org.openmrs.Concept;

/**
 * Represents a Concept configured as a configurable option within the HTML Form Schema
 */
public class ConceptOption {
	
	private String displayName;
	
	private Concept concept;
	
	public ConceptOption() {
	}
	
	public ConceptOption(String displayName, Concept concept) {
		this.displayName = displayName;
		this.concept = concept;
	}
	
	/**
	 * Gets the display name for this answer
	 * 
	 * @return the displayName
	 */
	public String getDisplayName() {
		return displayName;
	}
	
	/**
	 * Sets the display name for this answer
	 * 
	 * @param displayName the displayName to set
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	
	/**
	 * Gets the concept associated with this answer
	 * 
	 * @return the concept
	 */
	public Concept getConcept() {
		return concept;
	}
	
	/**
	 * Sets the concept associated with this answer
	 * 
	 * @param concept the concept to set
	 */
	public void setConcept(Concept concept) {
		this.concept = concept;
	}
}

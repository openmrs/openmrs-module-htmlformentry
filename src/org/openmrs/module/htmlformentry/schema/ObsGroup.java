package org.openmrs.module.htmlformentry.schema;

import java.util.ArrayList;
import java.util.List;

import org.openmrs.Concept;

/**
 * Represents an ObsGroup in the HTML Form Schema
 */
public class ObsGroup implements HtmlFormField {

	private Concept concept;
	private List<ObsField> children = new ArrayList<ObsField>();
    
    public ObsGroup(Concept concept) {
    	this.concept = concept;
    }

	/**
	 * Gets the parent concept associated with this ObsGroup
	 * 
	 * @return the concept
	 */
	public Concept getConcept() {
		return concept;
	}

	/**
	 * Sets the parent concept associated with this ObsGroup
	 * 
	 * @param concept the concept to set
	 */
	public void setConcept(Concept concept) {
		this.concept = concept;
	}

	/**
	 * Gets the Obs fields that are members of this group
	 * 
	 * return the children
	 */
	public List<ObsField> getChildren() {
		return children;
	}

	/**
	 * Sets the Obs fields that are members of this group
	 * 
	 * @param children the children to set
	 */
	public void setChildren(List<ObsField> children) {
		this.children = children;
	}
}

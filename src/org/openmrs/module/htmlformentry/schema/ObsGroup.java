package org.openmrs.module.htmlformentry.schema;

import java.util.ArrayList;
import java.util.List;

import org.openmrs.Concept;

public class ObsGroup implements HtmlFormField {

	private Concept concept;
	private List<ObsField> children = new ArrayList<ObsField>();
    
    public ObsGroup(Concept concept) {
    	this.concept = concept;
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

	/**
	 * @return the children
	 */
	public List<ObsField> getChildren() {
		return children;
	}

	/**
	 * @param children the children to set
	 */
	public void setChildren(List<ObsField> children) {
		this.children = children;
	}
}

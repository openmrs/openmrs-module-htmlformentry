package org.openmrs.module.htmlformentry.schema;

import org.openmrs.Concept;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents an ObsGroup in the HTML Form Schema
 */
public class ObsGroup implements HtmlFormField {
	
	private Concept concept;
	
	private List<HtmlFormField> children = new ArrayList<HtmlFormField>();
	
	private String label;
	
	private String controlId;
	
	private Map.Entry<Concept, Concept> hiddenObs;
	
	public ObsGroup(Concept concept) {
		this.concept = concept;
	}
	
	public ObsGroup(Concept concept, String label) {
		this.concept = concept;
		this.label = label;
	}
	
	public ObsGroup(Concept concept, String label, String controlId) {
		this.concept = concept;
		this.label = label;
		this.controlId = controlId;
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
	
	public List<HtmlFormField> getChildren() {
		return children;
	}
	
	public void setChildren(List<HtmlFormField> children) {
		for (HtmlFormField hff : children) {
			if (hff instanceof ObsField == false && hff instanceof ObsGroup == false)
				throw new RuntimeException("You can only add an ObsField or an ObsGroup to an ObsGroup");
		}
		this.children = children;
	}
	
	public void addChild(HtmlFormField hff) {
		if (hff instanceof ObsField || hff instanceof ObsGroup)
			this.children.add(hff);
		else
			throw new RuntimeException("You can only add an ObsField or an ObsGroup to an ObsGroup");
	}
	
	public String getLabel() {
		return label;
	}
	
	public void setLabel(String label) {
		this.label = label;
	}
	
	public String getControlId() {
		return controlId;
	}
	
	public void setControlId(String controlId) {
		this.controlId = controlId;
	}
	
	public Map.Entry<Concept, Concept> gethiddenObs() {
		return hiddenObs;
	}
	
	public void sethiddenObs(Map.Entry<Concept, Concept> hiddenObs) {
		this.hiddenObs = hiddenObs;
	}
	
}

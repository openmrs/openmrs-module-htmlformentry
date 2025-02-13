package org.openmrs.module.htmlformentry.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.openmrs.Concept;

/**
 * Represents an ObsGroup in the HTML Form Schema
 */
public class ObsGroup implements HtmlFormField {
	
	private Concept concept;
	
	private List<HtmlFormField> children = new ArrayList<HtmlFormField>();
	
	private String label;
	
	// the following fields are used to store a hidden obs that may be associated with this obs group
	private Concept hiddenQuestion;
	
	private Object hiddenAnswer;
	
	public ObsGroup(Concept concept) {
		this.concept = concept;
	}
	
	public ObsGroup(Concept concept, String label) {
		this.concept = concept;
		this.label = label;
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
	
	public Concept getHiddenQuestion() {
		return hiddenQuestion;
	}
	
	public void setHiddenQuestion(Concept hiddenQuestion) {
		this.hiddenQuestion = hiddenQuestion;
	}
	
	public Object getHiddenAnswer() {
		return hiddenAnswer;
	}
	
	public void setHiddenAnswer(Object hiddenAnswer) {
		this.hiddenAnswer = hiddenAnswer;
	}
}

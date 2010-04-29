package org.openmrs.module.htmlformentry.schema;

import java.util.ArrayList;
import java.util.List;

import org.openmrs.Concept;

/**
 * Represents an Obs field in an HTML Form schema
 */
public class ObsField implements HtmlFormField {

	private String name;
	private Concept question;
	private List<ObsFieldAnswer> answers = new ArrayList<ObsFieldAnswer>();
    
    public ObsField() { }

	/**
	 * Gets the name of the field
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name of the field
	 * 
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gets the question associated with the field
	 * 
	 * @return the question
	 */
	public Concept getQuestion() {
		return question;
	}

	/**
	 * Sets the question associated with the field
	 * 
	 * @param question the question to set
	 */
	public void setQuestion(Concept question) {
		this.question = question;
	}

	/**
	 * Gets the answers associated with the field
	 * 
	 * @return the answers
	 */
	public List<ObsFieldAnswer> getAnswers() {
		return answers;
	}

	/**
	 * Sets the answers associated with the field
	 * 
	 * @param answers the answers to set
	 */
	public void setAnswers(List<ObsFieldAnswer> answers) {
		this.answers = answers;
	}
}

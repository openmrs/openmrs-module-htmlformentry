package org.openmrs.module.htmlformentry.schema;

import java.util.ArrayList;
import java.util.List;

import org.openmrs.Concept;

public class ObsField implements HtmlFormField {

	private String name;
	private Concept question;
	private List<ObsFieldAnswer> answers = new ArrayList<ObsFieldAnswer>();
    
    public ObsField() { }

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the question
	 */
	public Concept getQuestion() {
		return question;
	}

	/**
	 * @param question the question to set
	 */
	public void setQuestion(Concept question) {
		this.question = question;
	}

	/**
	 * @return the answers
	 */
	public List<ObsFieldAnswer> getAnswers() {
		return answers;
	}

	/**
	 * @param answers the answers to set
	 */
	public void setAnswers(List<ObsFieldAnswer> answers) {
		this.answers = answers;
	}
}

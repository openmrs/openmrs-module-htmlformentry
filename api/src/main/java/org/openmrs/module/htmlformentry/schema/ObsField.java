package org.openmrs.module.htmlformentry.schema;

import org.openmrs.Concept;
import org.openmrs.Obs;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an Obs field in an HTML Form schema
 */
public class ObsField implements HtmlFormField {

	private String name;
	private Concept question;
	private List<ObsFieldAnswer> answers = new ArrayList<ObsFieldAnswer>();
	private List<ObsFieldAnswer> questions = new ArrayList<ObsFieldAnswer>(); //for concept selects
    private Obs existingObs; // any obs currently associated with this field
    
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

	/**
	 * 
	 * gets the possible questions when doing a concept select
	 * 
	 * @return List<ObsFieldAnswer>
	 */
	public List<ObsFieldAnswer> getQuestions() {
		return questions;
	}
	
	/**
	 * 
	 * sets the possible questions when doing a concept select
	 * 
	 * @param questions
	 */
	public void setQuestions(List<ObsFieldAnswer> questions) {
		this.questions = questions;
	}

    /**
     * If used with the context of an existing form/encounter, any existing obs associated with this field is stored here
     */
    public Obs getExistingObs() {
        return existingObs;
    }

    /**
     * If used with the context of an existing form/encounter, any existing obs associated with this field is stored here
     */
    public void setExistingObs(Obs existingObs) {
        this.existingObs = existingObs;
    }
}

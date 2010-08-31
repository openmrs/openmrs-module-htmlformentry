package org.openmrs.module.htmlformentry;

import java.util.List;
import java.util.Set;

import org.openmrs.Concept;
import org.openmrs.Obs;

/** 
 * Data object that represents a single component of an ObsGroup
 */
public class ObsGroupComponent {

	private Concept question;
    
	private Concept answer;
    
    public ObsGroupComponent() {
    }

    public ObsGroupComponent(Concept question, Concept answer) {
        this.question = question;
        this.answer = answer;
    }


    /** Gets the concept that represents the component's question */
    public Concept getQuestion() {
        return question;
    }

    /** Sets the concept that represents the component's question */
    public void setQuestion(Concept question) {
        this.question = question;
    }

    /** Gets the concept that represents the component's answer */
    public Concept getAnswer() {
        return answer;
    }

    /** Sets the concept that represents the component's answer */
    public void setAnswer(Concept answer) {
        this.answer = answer;
    }

    /**
     * Returns true if the Obs that are members of group are a subset of the Obs in questionsAndAnswers 
     * 
     * @param questionsAndAnswers
     * @param group
     * @return
     */
    public static boolean supports(List<ObsGroupComponent> questionsAndAnswers, Obs parentObs, Set<Obs> group) {
        for (Obs obs : group) {
            boolean match = false;
            for (ObsGroupComponent test : questionsAndAnswers) {
                boolean questionMatches = test.getQuestion().getConceptId().equals(obs.getConcept().getConceptId());
                boolean answerMatches = (test.getAnswer() == null) 
                    ||(obs.getValueCoded() != null && test.getAnswer().getConceptId().equals(obs.getValueCoded().getConceptId()));
                if (questionMatches && answerMatches) {
                    match = true;
                    break;
                }
            }
            if (!match)
                return false;
        }
        return true;
    }
    
}

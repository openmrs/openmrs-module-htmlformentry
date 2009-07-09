package org.openmrs.module.htmlformentry;

import java.util.List;

import org.openmrs.Concept;
import org.openmrs.Obs;

public class ObsGroupComponent {

    private Concept question;
    private Concept answer;
    
    public ObsGroupComponent() {
    }

    public ObsGroupComponent(Concept question, Concept answer) {
        this.question = question;
        this.answer = answer;
    }
    
    public Concept getQuestion() {
        return question;
    }

    public void setQuestion(Concept question) {
        this.question = question;
    }

    public Concept getAnswer() {
        return answer;
    }

    public void setAnswer(Concept answer) {
        this.answer = answer;
    }

    /**
     * Returns true if the Obs that are members of group are a subset of the allowed obs in questionsAndAnswers 
     * 
     * @param questionsAndAnswers
     * @param group
     * @return
     */
    public static boolean supports(List<ObsGroupComponent> questionsAndAnswers, Obs group) {
        // TODO handle multi-level obs groups
        for (Obs obs : group.getGroupMembers()) {
            boolean match = false;
            for (ObsGroupComponent test : questionsAndAnswers) {
                boolean questionMatches = test.getQuestion().getConceptId().equals(obs.getConcept().getConceptId());
                boolean answerMatches = test.getAnswer() == null ||
                    (obs.getValueCoded() != null &&
                            test.getAnswer().getConceptId().equals(obs.getValueCoded().getConceptId()));
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

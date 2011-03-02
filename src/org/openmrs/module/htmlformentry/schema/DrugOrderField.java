package org.openmrs.module.htmlformentry.schema;

import java.util.ArrayList;
import java.util.List;

import org.openmrs.Concept;

public class DrugOrderField implements HtmlFormField{
    private List<DrugOrderAnswer> drugOrderAnswers;
    private Concept discontinuedReasonQuestion;
    private List<ObsFieldAnswer> discontinuedReasonAnswers = new ArrayList<ObsFieldAnswer>();
    
    public DrugOrderField() {}

    public List<DrugOrderAnswer> getDrugOrderAnswers() {
        return drugOrderAnswers;
    }

    public void setDrugOrderAnswers(List<DrugOrderAnswer> drugOrderAnswers) {
        this.drugOrderAnswers = drugOrderAnswers;
    }
    
    public void addDrugOrderAnswer(DrugOrderAnswer doa){
        if (drugOrderAnswers == null){
            drugOrderAnswers = new ArrayList<DrugOrderAnswer>();
        }
        drugOrderAnswers.add(doa);
    }

    public Concept getDiscontinuedReasonQuestion() {
        return discontinuedReasonQuestion;
    }

    public void setDiscontinuedReasonQuestion(Concept discontinuedReasonQuestion) {
        this.discontinuedReasonQuestion = discontinuedReasonQuestion;
    }

    public List<ObsFieldAnswer> getDiscontinuedReasonAnswers() {
        return discontinuedReasonAnswers;
    }

    public void setDiscontinuedReasonAnswers(
            List<ObsFieldAnswer> discontinuedReasonAnswers) {
        this.discontinuedReasonAnswers = discontinuedReasonAnswers;
    }
    public void addDiscontinuedReasonAnswer(ObsFieldAnswer ofa){
        if (discontinuedReasonAnswers == null)
            discontinuedReasonAnswers = new ArrayList<ObsFieldAnswer>();
        discontinuedReasonAnswers.add(ofa);
        
    }
}

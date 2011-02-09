package org.openmrs.module.htmlformentry.schema;

import java.util.ArrayList;
import java.util.List;

public class DrugOrderField implements HtmlFormField{
    private List<DrugOrderAnswer> drugOrderAnswers;
    
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
    
}

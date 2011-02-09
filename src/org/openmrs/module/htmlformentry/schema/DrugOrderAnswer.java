package org.openmrs.module.htmlformentry.schema;

import org.openmrs.Drug;

public class DrugOrderAnswer {

    private String displayName;
    private Drug drug;
    
    public DrugOrderAnswer(){}
    public DrugOrderAnswer(Drug drug, String displayName){
        this.drug = drug;
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Drug getDrug() {
        return drug;
    }

    public void setDrug(Drug drug) {
        this.drug = drug;
    }
    
    
}

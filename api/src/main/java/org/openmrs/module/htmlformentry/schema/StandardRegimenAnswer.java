package org.openmrs.module.htmlformentry.schema;

import org.openmrs.order.RegimenSuggestion;


public class StandardRegimenAnswer {
	

	 private RegimenSuggestion regimenSuggestion;
	    
	 public StandardRegimenAnswer(){}
	 
	 public StandardRegimenAnswer(RegimenSuggestion rs){
		 this.regimenSuggestion = rs;
	 }

	
    public RegimenSuggestion getRegimenSuggestion() {
    	return regimenSuggestion;
    }

	
    public void setRegimenSuggestion(RegimenSuggestion regimenSuggestion) {
    	this.regimenSuggestion = regimenSuggestion;
    }
	 
	 
	 
	    
}

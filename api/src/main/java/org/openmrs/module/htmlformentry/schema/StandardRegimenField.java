package org.openmrs.module.htmlformentry.schema;

import java.util.ArrayList;
import java.util.List;

import org.openmrs.Concept;


public class StandardRegimenField implements HtmlFormField {

	 	private List<StandardRegimenAnswer> standardRegimenAnswers;
	    private Concept discontinuedReasonQuestion;
	    private List<ObsFieldAnswer> discontinuedReasonAnswers = new ArrayList<ObsFieldAnswer>();
	    
	    public StandardRegimenField(){}
	    
	    public List<StandardRegimenAnswer> getStandardRegimenAnswers() {
	        return standardRegimenAnswers;
	    }
	    
        public void setStandardRegimenAnswers(List<StandardRegimenAnswer> standardRegimenAnswers) {
        	this.standardRegimenAnswers = standardRegimenAnswers;
        }
        
        public void addStandardRegimenAnswer(StandardRegimenAnswer sra){
        	if (standardRegimenAnswers == null)
        		standardRegimenAnswers = new ArrayList<StandardRegimenAnswer>();
        	standardRegimenAnswers.add(sra);
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

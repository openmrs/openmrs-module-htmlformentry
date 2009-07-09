package org.openmrs.module.htmlformentry.action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;

public abstract class RepeatControllerAction implements FormSubmissionControllerAction {

    protected List<FormSubmissionControllerAction> repeatingActions = new ArrayList<FormSubmissionControllerAction>();
    
    public void addAction(FormSubmissionControllerAction action) {
        repeatingActions.add(action);
    }
    
    public void beforeHandleSubmission(FormEntrySession session, HttpServletRequest submission) { }
    
    public void afterHandleSubmission(FormEntrySession session, HttpServletRequest submission) { }
    
    public void beforeValidateSubmission(FormEntryContext context, HttpServletRequest submission) { }
    
    public void afterValidateSubmission(FormEntryContext context, HttpServletRequest submission) { }
   
    public void handleSubmission(FormEntrySession session, HttpServletRequest submission) {
        beforeHandleSubmission(session, submission);
        for (FormSubmissionControllerAction action : repeatingActions)
            action.handleSubmission(session, submission);
        afterHandleSubmission(session, submission);
    }    

    public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {
        beforeValidateSubmission(context, submission);
        Collection<FormSubmissionError> ret = new ArrayList<FormSubmissionError>();
        for (FormSubmissionControllerAction action : repeatingActions) {
            Collection<FormSubmissionError> temp = action.validateSubmission(context, submission);
            if (temp != null)
                ret.addAll(temp);
        }
        afterValidateSubmission(context, submission);
        return ret;
    }
    
    

}

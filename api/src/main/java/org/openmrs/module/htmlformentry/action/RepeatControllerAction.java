package org.openmrs.module.htmlformentry.action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;

/**
 * Abstract class which defines a controller that can hold a set of Form Submission Controller Actions, and is itself a 
 * Form Submission Controller Action.
 */
public abstract class RepeatControllerAction implements FormSubmissionControllerAction {

    protected List<FormSubmissionControllerAction> repeatingActions = new ArrayList<FormSubmissionControllerAction>();
    
    /**
     * Associates a Form Submission Controller Action with this Repeat Controller
     * 
     * @param action
     */
    public void addAction(FormSubmissionControllerAction action) {
        repeatingActions.add(action);
    }
    
    /**
     * Performs any actions that need to happen before handling a submission
     * 
     * @param session
     * @param submission
     */
    public void beforeHandleSubmission(FormEntrySession session, HttpServletRequest submission) { }
    
    /**
     * Performs any actions that need to happen after handling a submission
     * 
     * @param session
     * @param submission
     */
    public void afterHandleSubmission(FormEntrySession session, HttpServletRequest submission) { }
    
    /**
     * Performs any actions that need to happen after handling a submission
     * 
     * @param context
     * @param submission
     */
    public void beforeValidateSubmission(FormEntryContext context, HttpServletRequest submission) { }
    
    /**
     * Performs any actions that need to happen after handling a submission
     * 
     * @param context
     * @param submission
     */
    public void afterValidateSubmission(FormEntryContext context, HttpServletRequest submission) { }
   
    /**
     * Calls the handleSubmission method for all Form Submission Controller Actions associated with this Repeat Controller
     * 
     * @param session
     * @param submission
     */
    @Override
    public void handleSubmission(FormEntrySession session, HttpServletRequest submission) {
        beforeHandleSubmission(session, submission);
        int numIterations = getNumberOfIterations(session.getContext(), submission);
        for (int i = 0; i < numIterations; ++i) {
        	session.getContext().setRepeatIteration(i);
	        for (FormSubmissionControllerAction action : repeatingActions) {
	            action.handleSubmission(session, submission);
	        }
        }
        session.getContext().setRepeatIteration(null);
        afterHandleSubmission(session, submission);
    }   
    
    /**
     * Calls the validateSubmission method for all Form Submission Controller Actions associated with this Repeat Controller
     * 
     * @param context
     * @param submission
     */
    @Override
    public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {
    	beforeValidateSubmission(context, submission);
        Collection<FormSubmissionError> ret = new ArrayList<FormSubmissionError>();
        int numIterations = getNumberOfIterations(context, submission);
        for (int i = 0; i < numIterations; ++i) {
        	context.setRepeatIteration(i);
	        for (FormSubmissionControllerAction action : repeatingActions) {
	            Collection<FormSubmissionError> temp = action.validateSubmission(context, submission);
	            if (temp != null)
	                ret.addAll(temp);
	        }
        }
        context.setRepeatIteration(null);
        afterValidateSubmission(context, submission);
        return ret;
    }

	/**
     * @param submission 
	 * @param context 
	 * @return the number of times we should iterate over the actions within this tag
     */
    protected abstract int getNumberOfIterations(FormEntryContext context, HttpServletRequest submission);    
    

}
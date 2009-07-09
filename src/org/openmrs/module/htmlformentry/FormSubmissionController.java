package org.openmrs.module.htmlformentry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.action.RepeatControllerAction;

/**
 * When going through XML/HTML substitution to build a form, one of these is created as a side-effect. It encapsulates how to validate the form, and commit it to the database
 */
public class FormSubmissionController {
    
    private List<FormSubmissionControllerAction> actions = new ArrayList<FormSubmissionControllerAction>();
    private transient List<FormSubmissionError> lastSubmissionErrors;
    private transient HttpServletRequest lastSubmission;
    private RepeatControllerAction repeat = null;
    
    public FormSubmissionController() {
    }
    
    public void startRepeat(RepeatControllerAction repeat) {
        if (this.repeat != null)
            throw new IllegalArgumentException("Nested Repeating elements are not yet implemented");
        addAction(repeat);
        this.repeat = repeat;
    }
    
    public void endRepeat() {
        if (this.repeat == null)
            throw new IllegalArgumentException("No Repeating element is open now");
        this.repeat = null;
    }
    
    public void addAction(FormSubmissionControllerAction action) {
        actions.add(action);
    }
    
    public List<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {
        lastSubmission = submission;
        lastSubmissionErrors = new ArrayList<FormSubmissionError>();
        for (FormSubmissionControllerAction element : actions) {
            Collection<FormSubmissionError> errs = element.validateSubmission(context, submission);
            if (errs != null)
                lastSubmissionErrors.addAll(errs);
        }
        return lastSubmissionErrors;
    }
    
    public void handleFormSubmission(FormEntrySession session, HttpServletRequest submission) {
        lastSubmission = submission;
        for (FormSubmissionControllerAction element : actions) {
            element.handleSubmission(session, submission);
        }
    }
    
    public HttpServletRequest getLastSubmission() {
        return lastSubmission;
    }

    public List<FormSubmissionError> getLastSubmissionErrors() {
        return lastSubmissionErrors;
    }
    
}

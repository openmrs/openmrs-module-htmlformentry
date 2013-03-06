package org.openmrs.module.htmlformentry.action;

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;

/**
 * Represents an action that should be taken on form submission. Often this is related to an element
 * on a form (e.g. an {@code <obs/>} element has one of these.)
 * <p/>
 * Typically these would be created when parsing through an HtmlForm and generating html. For example
 * an <obs conceptId="123"/> element would both generate html and also create a FormSubmissionControllerAction
 * that knows how to validate and handle its submission.  
 */
public interface FormSubmissionControllerAction {

    /**
     * Validates this element in the form, returning all errors if validation fails.
     * 
     * @param context
     * @param submission
     * @return
     */
    public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission);

    /**
     * Handles the submission of this element in the form. Assumes that validateSubmission has been called and returned no errors.
     * 
     * @param session
     * @param submission
     */
    public void handleSubmission(FormEntrySession session, HttpServletRequest submission);

}

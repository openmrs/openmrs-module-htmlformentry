package org.openmrs.module.htmlformentry.element;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.Program;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;

/**
 * Serves as both the HtmlGeneratorElement and the FormSubmissionControllerAction
 * for a Program Enrollment.
 */
public class CompleteProgramElement implements HtmlGeneratorElement, FormSubmissionControllerAction {
	
	private Program program;

	public CompleteProgramElement(FormEntryContext context, Map<String, String> parameters) {
		try {
			program = HtmlFormEntryUtil.getProgram(parameters.get("programId"));
		} catch (Exception ex) {
			throw new IllegalArgumentException("Couldn't find program in: " + parameters);
		}
    }

	/**
	 * @see org.openmrs.module.htmlformentry.element.HtmlGeneratorElement#generateHtml(org.openmrs.module.htmlformentry.FormEntryContext)
	 */
	@Override
    public String generateHtml(FormEntryContext context) {
	    return "";
    }

	/**
	 * @see org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction#handleSubmission(org.openmrs.module.htmlformentry.FormEntrySession, javax.servlet.http.HttpServletRequest)
	 */
	@Override
    public void handleSubmission(FormEntrySession session, HttpServletRequest submission) {
	    if (session.getContext().getMode() == Mode.ENTER) {
	    	session.getSubmissionActions().completeProgram(program);
	    }
    }

	/**
	 * @see org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction#validateSubmission(org.openmrs.module.htmlformentry.FormEntryContext, javax.servlet.http.HttpServletRequest)
	 */
	@Override
    public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {
	    return Collections.emptySet();
    }

}

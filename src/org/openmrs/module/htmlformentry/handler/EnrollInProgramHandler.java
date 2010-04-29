package org.openmrs.module.htmlformentry.handler;

import java.util.Map;

import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.element.EnrollInProgramElement;

/**
 * Handles the {@code <enrollInProgram>} tag
 */
public class EnrollInProgramHandler extends SubstitutionTagHandler implements TagHandler {
	
	@Override
	protected String getSubstitution(FormEntrySession session, FormSubmissionController controllerActions,
	                                 Map<String, String> parameters) {
		EnrollInProgramElement element = new EnrollInProgramElement(session.getContext(), parameters);
        session.getSubmissionController().addAction(element);
        
        return element.generateHtml(session.getContext());
	}
	
}

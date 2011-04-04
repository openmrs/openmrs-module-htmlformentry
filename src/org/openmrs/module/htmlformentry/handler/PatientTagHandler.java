package org.openmrs.module.htmlformentry.handler;

import java.util.Map;

import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.element.PatientDetailSubmissionElement;

/**
 * Handles the {@code <patient>} tag
 */
public class PatientTagHandler extends SubstitutionTagHandler {
	
	/**
	 * @see org.openmrs.module.htmlformentry.handler.SubstitutionTagHandler#getSubstitution(org.openmrs.module.htmlformentry.FormEntrySession, org.openmrs.module.htmlformentry.FormSubmissionController, java.util.Map)
	 */
	protected String getSubstitution(FormEntrySession session, FormSubmissionController controllerActions,
			Map<String, String> parameters) {
		
		PatientDetailSubmissionElement element = new PatientDetailSubmissionElement(session.getContext(), parameters);
		session.getSubmissionController().addAction(element);

		return element.generateHtml(session.getContext());
	}
}

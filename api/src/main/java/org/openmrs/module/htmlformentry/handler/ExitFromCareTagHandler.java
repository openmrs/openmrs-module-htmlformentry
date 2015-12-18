package org.openmrs.module.htmlformentry.handler;


import java.util.Map;

import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.element.ExitFromCareSubmissionElement;

/**
 * Handles the {@code <exitFromCare>} tag
 */
public class ExitFromCareTagHandler extends SubstitutionTagHandler{


    /**
	 * @see org.openmrs.module.htmlformentry.handler.SubstitutionTagHandler#getSubstitution(org.openmrs.module.htmlformentry.FormEntrySession, org.openmrs.module.htmlformentry.FormSubmissionController, java.util.Map)
	 */
    @Override
    protected String getSubstitution(FormEntrySession session, FormSubmissionController controllerActions,
                                Map<String, String> parameters) throws BadFormDesignException {

        ExitFromCareSubmissionElement element = new ExitFromCareSubmissionElement(session.getContext(), parameters);
		session.getSubmissionController().addAction(element);

		return element.generateHtml(session.getContext());
    }
}

package org.openmrs.module.htmlformentry.handler;

import java.util.Map;

import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.element.ObsSubmissionElement;

/**
 * Handles the {@code <obs>} tag
 */
public class ObsTagHandler extends SubstitutionTagHandler {

    @Override
    protected String getSubstitution(FormEntrySession session, FormSubmissionController controllerActions, Map<String, String> parameters) {
        ObsSubmissionElement element = new ObsSubmissionElement(session.getContext(), parameters);
        session.getSubmissionController().addAction(element);
        
        return element.generateHtml(session.getContext());
    }

}

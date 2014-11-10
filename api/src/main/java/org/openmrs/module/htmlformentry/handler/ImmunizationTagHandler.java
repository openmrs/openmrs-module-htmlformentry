package org.openmrs.module.htmlformentry.handler;

import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.element.ImmunizationSubmissionElement;

import java.util.Map;

/**
 * Tag handler for <immunization .. />
 */
public class ImmunizationTagHandler extends SubstitutionTagHandler {

    @Override
    protected String getSubstitution(FormEntrySession session, FormSubmissionController submissionController, Map<String, String> parameters) throws BadFormDesignException {
        ImmunizationSubmissionElement element = new ImmunizationSubmissionElement(session.getContext(), parameters);
        submissionController.addAction(element);
        return element.generateHtml(session.getContext());
    }
}
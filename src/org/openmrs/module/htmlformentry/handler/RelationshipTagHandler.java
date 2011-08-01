package org.openmrs.module.htmlformentry.handler;

import java.util.Map;

import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.element.DrugOrderSubmissionElement;
import org.openmrs.module.htmlformentry.element.RelationshipSubmissionElement;

/**
 * Handles the {@code <relationship>} tag
 */
public class RelationshipTagHandler extends SubstitutionTagHandler {

    @Override
    protected String getSubstitution(FormEntrySession session, FormSubmissionController controllerActions,
            Map<String, String> parameters) {
    	
		RelationshipSubmissionElement element = new RelationshipSubmissionElement(session.getContext(), parameters);
		session.getSubmissionController().addAction(element);
		
		return element.generateHtml(session.getContext());
    }

}

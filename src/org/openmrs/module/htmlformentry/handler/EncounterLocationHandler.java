package org.openmrs.module.htmlformentry.handler;

import java.util.HashMap;
import java.util.Map;

import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.element.EncounterDetailSubmissionElement;

/**
 * Handles the {@code <encounterLocation>} tag
 */
public class EncounterLocationHandler extends SubstitutionTagHandler {

    @Override
    protected String getSubstitution(FormEntrySession session, FormSubmissionController controllerActions,
            Map<String, String> parameters) {

        Map<String, Object> temp = new HashMap<String, Object>();
        temp.putAll(parameters);
        temp.put("location", true);
        EncounterDetailSubmissionElement element = new EncounterDetailSubmissionElement(session.getContext(), temp);
        session.getSubmissionController().addAction(element);
        
        return element.generateHtml(session.getContext());
    }

}

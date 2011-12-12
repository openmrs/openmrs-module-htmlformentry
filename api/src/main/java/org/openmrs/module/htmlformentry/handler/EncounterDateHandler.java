package org.openmrs.module.htmlformentry.handler;


import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.element.EncounterDetailSubmissionElement;

/**
 * Handles the {@code <encounterDate>} tag
 */
public class EncounterDateHandler extends SubstitutionTagHandler {

    @Override
    protected String getSubstitution(FormEntrySession session, FormSubmissionController controllerActions,
            Map<String, String> parameters) {
        Date date = null;
        if (parameters.get("default") != null) {
          	date = HtmlFormEntryUtil.translateDatetimeParam(parameters.get("default"), "yyyy-MM-dd");
        }
        Map<String, Object> temp = new HashMap<String, Object>();
        temp.putAll(parameters);
        temp.put("date", true);
        temp.put("defaultDate", date);
        EncounterDetailSubmissionElement element = new EncounterDetailSubmissionElement(session.getContext(), temp);
        session.getSubmissionController().addAction(element);
        
        return element.generateHtml(session.getContext());
    }

}

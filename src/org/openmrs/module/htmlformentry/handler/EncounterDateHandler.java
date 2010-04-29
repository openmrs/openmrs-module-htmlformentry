package org.openmrs.module.htmlformentry.handler;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
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
            if ("today".equals(parameters.get("default"))) {
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                date = cal.getTime();
            } else if ("now".equals(parameters.get("default"))) {
                date = new Date();
            } else {
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                try {
                    date = df.parse(parameters.get("default"));
                } catch (ParseException ex) {
                    throw new IllegalArgumentException(ex);
                }
            }
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

package org.openmrs.module.htmlformentry.handler;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.openmrs.Location;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.element.EncounterDetailSubmissionElement;

/**
 * Handles the {@code <encounterDate>} tag
 */
public class EncounterDateHandler extends SubstitutionTagHandler {
	
	@Override
	public String getName() {
		return "Encounter Date";
	}
	
	@Override
	public String getDescription() {
		return "Data entry widget for recording encounter date. Along with encounterLocation and encounterProvider, encounterDate should be present on every HTML form.";
	}
	
	protected List<AttributeDescriptor> createAttributeDescriptors() {
		List<AttributeDescriptor> attributeDescriptors = new ArrayList<AttributeDescriptor>();
		attributeDescriptors.add(new AttributeDescriptor("default", "Default", false, "Specifies default value for encounterDate widget.", "text"));
		attributeDescriptors.add(new AttributeDescriptor("showTime", "Show Time", false, "The showTime attribute determines if the encounterDate widget is date-only or date and time.", "boolean"));
		attributeDescriptors.add(new AttributeDescriptor("disallowMultipleEncountersOnDate", "Disallow Multiple Encounters", false, "This will warn the user that this Form type has been entered for this Patient on this Encounter date.  This will prevent duplicate paper entries of the same form.  The mechanism for this is an ajax popup that is presented to the user after selecting an encounter date (only if there is already a form submitted of the same type for that date for that patient).", "text"));
		return attributeDescriptors;
	}
	
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
        Map<String, Object> temp = new CaseInsensitiveMap();
        temp.putAll(parameters);
        temp.put("date", true);
        temp.put("defaultDate", date);
        EncounterDetailSubmissionElement element = new EncounterDetailSubmissionElement(session.getContext(), temp);
        session.getSubmissionController().addAction(element);
        
        return element.generateHtml(session.getContext());
    }

}

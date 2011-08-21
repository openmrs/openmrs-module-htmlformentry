package org.openmrs.module.htmlformentry.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.openmrs.Location;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.element.EncounterDetailSubmissionElement;

/**
 * Handles the {@code <encounterLocation>} tag
 */
public class EncounterLocationHandler extends SubstitutionTagHandler {

	@Override
	public String getName() {
		return "Encounter Location";
	}
	
	@Override
	public String getDescription() {
		return "Data entry widget for recording location of the encounter. Along with encounterDate and encounterProvider, encounterLocation should be present on every HTML form.";
	}
	
	protected List<AttributeDescriptor> createAttributeDescriptors() {
		List<AttributeDescriptor> attributeDescriptors = new ArrayList<AttributeDescriptor>();
		attributeDescriptors.add(new AttributeDescriptor("order", Location.class, "Order", false, "Determines which locations appear in the list, and specifies the order in which they appear.", "text"));
		attributeDescriptors.add(new AttributeDescriptor("default", Location.class, "Default", false, "Sets default value for the widget.", "text"));
		return Collections.unmodifiableList(attributeDescriptors);
	}
	
    @Override
    protected String getSubstitution(FormEntrySession session, FormSubmissionController controllerActions,
            Map<String, String> parameters) {

        Map<String, Object> temp = new CaseInsensitiveMap();
        temp.putAll(parameters);
        temp.put("location", true);
        EncounterDetailSubmissionElement element = new EncounterDetailSubmissionElement(session.getContext(), temp);
        session.getSubmissionController().addAction(element);
        
        return element.generateHtml(session.getContext());
    }

}

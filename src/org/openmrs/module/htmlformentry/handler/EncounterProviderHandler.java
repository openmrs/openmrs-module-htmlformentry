package org.openmrs.module.htmlformentry.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.openmrs.Location;
import org.openmrs.Person;
import org.openmrs.Role;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.element.EncounterDetailSubmissionElement;

/**
 * Handles the {@code <encounterProvider>} tag
 */
public class EncounterProviderHandler extends SubstitutionTagHandler {

	@Override
	public String getName() {
		return "Encounter Provider";
	}
	
	@Override
	public String getDescription() {
		return "Data entry widget for recording provider for the encounter. Along with encounterDate and encounterLocation, encounterProvider should be present on every HTML form.";
	}
	
	protected List<AttributeDescriptor> createAttributeDescriptors() {
		List<AttributeDescriptor> attributeDescriptors = new ArrayList<AttributeDescriptor>();
		attributeDescriptors.add(new AttributeDescriptor("default", Person.class, "Default", false, "Sets default value for the widget.", "text"));
		attributeDescriptors.add(new AttributeDescriptor("role", Role.class, "Role", false, "Filters the list of persons to only those users with the specified role.", "text"));
		attributeDescriptors.add(new AttributeDescriptor("persons", "Persons", false, "Determines which persons appear in the list, and specifies the order in which they appear.", "text"));
		return Collections.unmodifiableList(attributeDescriptors);
	}
	
    @Override
    protected String getSubstitution(FormEntrySession session, FormSubmissionController controllerActions,
            Map<String, String> parameters) {
        
        Map<String, Object> temp = new CaseInsensitiveMap();
        temp.putAll(parameters);
        temp.put("provider", true);
        EncounterDetailSubmissionElement element = new EncounterDetailSubmissionElement(session.getContext(), temp);
        session.getSubmissionController().addAction(element);
        
        return element.generateHtml(session.getContext());
    }

}

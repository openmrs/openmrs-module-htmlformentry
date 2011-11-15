package org.openmrs.module.htmlformentry.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.openmrs.PatientIdentifierType;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.element.PatientDetailSubmissionElement;

/**
 * Handles the {@code <patient>} tag
 */
public class PatientTagHandler extends SubstitutionTagHandler {
	
	@Override
    protected List<AttributeDescriptor> createAttributeDescriptors() {
		List<AttributeDescriptor> attributeDescriptors = new ArrayList<AttributeDescriptor>();	
		attributeDescriptors.add(new AttributeDescriptor("identifierTypeId", PatientIdentifierType.class));
		return Collections.unmodifiableList(attributeDescriptors);
	}
	
	/**
	 * @see org.openmrs.module.htmlformentry.handler.SubstitutionTagHandler#getSubstitution(org.openmrs.module.htmlformentry.FormEntrySession, org.openmrs.module.htmlformentry.FormSubmissionController, java.util.Map)
	 */
	@Override
    protected String getSubstitution(FormEntrySession session, FormSubmissionController controllerActions,
			Map<String, String> parameters) {
		
		PatientDetailSubmissionElement element = new PatientDetailSubmissionElement(session.getContext(), parameters);
		session.getSubmissionController().addAction(element);

		return element.generateHtml(session.getContext());
	}
}

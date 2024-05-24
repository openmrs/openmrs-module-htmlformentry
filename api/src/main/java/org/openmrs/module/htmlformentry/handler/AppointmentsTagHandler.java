package org.openmrs.module.htmlformentry.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.element.AppointmentsElement;

public class AppointmentsTagHandler extends SubstitutionTagHandler {
	
	@Override
	protected List<AttributeDescriptor> createAttributeDescriptors() {
		List<AttributeDescriptor> attributeDescriptors = new ArrayList<AttributeDescriptor>();
		return Collections.unmodifiableList(attributeDescriptors);
	}
	
	@Override
	protected String getSubstitution(FormEntrySession session, FormSubmissionController controllerActions,
	        Map<String, String> parameters) {
		AppointmentsElement element = new AppointmentsElement(session.getContext(), parameters);
		session.getSubmissionController().addAction(element);
		return element.generateHtml(session.getContext());
	}
}

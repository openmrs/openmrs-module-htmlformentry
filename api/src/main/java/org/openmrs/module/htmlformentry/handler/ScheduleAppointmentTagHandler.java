package org.openmrs.module.htmlformentry.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.element.ScheduleAppointmentElement;

public class ScheduleAppointmentTagHandler extends SubstitutionTagHandler {

	@Override
	protected List<AttributeDescriptor> createAttributeDescriptors() {
		return Collections.unmodifiableList(new ArrayList<AttributeDescriptor>());
	}

	@Override
	protected String getSubstitution(FormEntrySession session, FormSubmissionController controllerActions,
	        Map<String, String> parameters) {
		ScheduleAppointmentElement element = new ScheduleAppointmentElement(session.getContext(), parameters);
		session.getSubmissionController().addAction(element);
		return element.generateHtml(session.getContext());
	}
}

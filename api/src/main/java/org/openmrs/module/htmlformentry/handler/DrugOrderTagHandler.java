package org.openmrs.module.htmlformentry.handler;

import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

/**
 * Handles the {@code <drugOrder>} tag
 */
public class DrugOrderTagHandler extends SubstitutionTagHandler {

	@Autowired
	DrugOrderTagHandlerSupport handler;

	@Override
	public List<AttributeDescriptor> createAttributeDescriptors() {
		return handler.createAttributeDescriptors();
	}

	@Override
	public String getSubstitution(FormEntrySession session, FormSubmissionController controllerActions,
								  Map<String, String> parameters) {
		return handler.getSubstitution(session, controllerActions, parameters);
	}
}

package org.openmrs.module.htmlformentry.handler;

import java.util.List;
import java.util.Map;

import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;

public interface StandardRegimenTagHandlerSupport {

	List<AttributeDescriptor> createAttributeDescriptors();

	String getSubstitution(FormEntrySession session,
			FormSubmissionController controllerActions,
			Map<String, String> parameters);

}

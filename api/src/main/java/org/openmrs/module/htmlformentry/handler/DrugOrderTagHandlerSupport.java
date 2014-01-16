package org.openmrs.module.htmlformentry.handler;

import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;

import java.util.List;
import java.util.Map;

/**
 * Provides an implementation for DrugOrderTagHandler. Bean implementing this interface will
 * be loaded based on the OpenMRS version.
 */
public interface DrugOrderTagHandlerSupport {

	List<AttributeDescriptor> createAttributeDescriptors();

	String getSubstitution(FormEntrySession session, FormSubmissionController controllerActions,
						   Map<String, String> parameters);
}

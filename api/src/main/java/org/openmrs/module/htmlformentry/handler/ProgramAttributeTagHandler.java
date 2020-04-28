package org.openmrs.module.htmlformentry.handler;

import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;

import java.util.Map;

/**
 * Handles the {@code <programAttribute>} tag
 */
public interface ProgramAttributeTagHandler {

	String getSubstitution(FormEntrySession session,
                           FormSubmissionController controllerActions,
                           Map<String, String> parameters);
}

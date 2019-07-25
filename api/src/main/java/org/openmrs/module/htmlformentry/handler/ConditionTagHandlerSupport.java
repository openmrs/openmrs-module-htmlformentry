package org.openmrs.module.htmlformentry.handler;

import java.util.Map;

import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;

public interface ConditionTagHandlerSupport {

	String getSubstitution(FormEntrySession session,
			FormSubmissionController controllerActions,
			Map<String, String> parameters);
}

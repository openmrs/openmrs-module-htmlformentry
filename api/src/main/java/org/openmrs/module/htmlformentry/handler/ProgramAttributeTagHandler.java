package org.openmrs.module.htmlformentry.handler;

import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

public class ProgramAttributeTagHandler extends SubstitutionTagHandler {

	@Autowired
	ProgramAttributeTagHandlerSupport handler;
	
	@Override
	protected String getSubstitution(FormEntrySession session, FormSubmissionController controllerActions,
			Map<String, String> parameters) throws BadFormDesignException {
		return handler.getSubstitution(session, controllerActions, parameters);
	}

}

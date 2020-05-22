package org.openmrs.module.htmlformentry.handler;

import java.util.Map;

import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.springframework.beans.factory.annotation.Autowired;

public class ConditionTagHandler extends SubstitutionTagHandler {
	
	@Autowired
	private ConditionTagHandlerSupport handler;
	
	@Override
	protected String getSubstitution(FormEntrySession session, FormSubmissionController controllerActions,
	        Map<String, String> parameters) throws BadFormDesignException {
		return handler.getSubstitution(session, controllerActions, parameters);
	}

	public ConditionTagHandlerSupport getHandler() {
		return handler;
	}

	public void setHandler(ConditionTagHandlerSupport handler) {
		this.handler = handler;
	}

}

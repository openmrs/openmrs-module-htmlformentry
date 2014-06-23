package org.openmrs.module.htmlformentry.handler;

import java.util.List;
import java.util.Map;

import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.springframework.beans.factory.annotation.Autowired;


public class StandardRegimenTagHandler extends SubstitutionTagHandler {

	@Autowired
	StandardRegimenTagHandlerSupport handler;
	
	@Override
    protected List<AttributeDescriptor> createAttributeDescriptors() {
		return handler.createAttributeDescriptors();
	}
	
    @Override
    protected String getSubstitution(FormEntrySession session, FormSubmissionController controllerActions,
            Map<String, String> parameters) {
    	return handler.getSubstitution(session, controllerActions, parameters);
    }
    
}

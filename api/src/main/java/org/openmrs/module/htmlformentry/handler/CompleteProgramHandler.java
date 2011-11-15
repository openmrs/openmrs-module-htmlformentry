package org.openmrs.module.htmlformentry.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.openmrs.Program;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.element.CompleteProgramElement;

/**
 * Handles the {@code <completeProgram>} tag
 */
public class CompleteProgramHandler extends SubstitutionTagHandler implements TagHandler {
	
	@Override
    protected List<AttributeDescriptor> createAttributeDescriptors() {
		List<AttributeDescriptor> attributeDescriptors = new ArrayList<AttributeDescriptor>();
		attributeDescriptors.add(new AttributeDescriptor("programId", Program.class));
		return Collections.unmodifiableList(attributeDescriptors);
	}
	
	@Override
	protected String getSubstitution(FormEntrySession session, FormSubmissionController controllerActions,
	                                 Map<String, String> parameters) {
		CompleteProgramElement element = new CompleteProgramElement(session.getContext(), parameters);
        session.getSubmissionController().addAction(element);
        
        return element.generateHtml(session.getContext());
	}
	
}

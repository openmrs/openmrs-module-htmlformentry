package org.openmrs.module.htmlformentry.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.openmrs.Program;
import org.openmrs.ProgramWorkflowState;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.element.EnrollInProgramElement;

/**
 * Handles the {@code <enrollInProgram>} tag
 */
public class EnrollInProgramHandler extends SubstitutionTagHandler implements TagHandler {
	
	@Override
	protected List<AttributeDescriptor> createAttributeDescriptors() {
		List<AttributeDescriptor> attributeDescriptors = new ArrayList<AttributeDescriptor>();
		attributeDescriptors.add(new AttributeDescriptor("programId", Program.class));
		attributeDescriptors.add(new AttributeDescriptor("stateIds", ProgramWorkflowState.class));
		return Collections.unmodifiableList(attributeDescriptors);
	}
	
	@Override
	protected String getSubstitution(FormEntrySession session, FormSubmissionController controllerActions,
	                                 Map<String, String> parameters) {
		EnrollInProgramElement element = new EnrollInProgramElement(session.getContext(), parameters);
		session.getSubmissionController().addAction(element);
		
		return element.generateHtml(session.getContext());
	}
	
}

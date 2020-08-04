package org.openmrs.module.htmlformentry.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.openmrs.Program;
import org.openmrs.ProgramWorkflowState;
import org.openmrs.annotation.OpenmrsProfile;
import org.openmrs.module.htmlformentry.EnrollInProgramElement2_2;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;

@OpenmrsProfile(openmrsPlatformVersion = "2.2.0")
public class EnrollInProgramHandler2_2 extends EnrollInProgramHandler {
	
	@Override
	protected List<AttributeDescriptor> createAttributeDescriptors() {
		List<AttributeDescriptor> attributeDescriptors = new ArrayList<AttributeDescriptor>();
		attributeDescriptors.add(new AttributeDescriptor("programId", Program.class));
		attributeDescriptors.add(new AttributeDescriptor("stateIds", ProgramWorkflowState.class));
		attributeDescriptors.add(new AttributeDescriptor("patientProgramAttributes", ProgramWorkflowState.class));
		return Collections.unmodifiableList(attributeDescriptors);
	}
	
	@Override
	protected String getSubstitution(FormEntrySession session, FormSubmissionController controllerActions,
	        Map<String, String> parameters) {
		EnrollInProgramElement2_2 element = new EnrollInProgramElement2_2(session.getContext(), parameters);
		session.getSubmissionController().addAction(element);
		
		return element.generateHtml(session.getContext());
	}
	
}

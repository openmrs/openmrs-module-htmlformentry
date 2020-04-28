package org.openmrs.module.htmlformentry.handler;

import org.openmrs.Program;
import org.openmrs.ProgramAttributeType;
import org.openmrs.annotation.OpenmrsProfile;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.ProgramAttributeElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@OpenmrsProfile(openmrsPlatformVersion = "2.2.0")
public class ProgramAttributeTagHandlerSupport2_2 extends SubstitutionTagHandler implements TagHandler {

	@Override
	protected List<AttributeDescriptor> createAttributeDescriptors() {
		List<AttributeDescriptor> attributeDescriptors = new ArrayList<AttributeDescriptor>();
		attributeDescriptors.add(new AttributeDescriptor("programId", Program.class));
		attributeDescriptors.add(new AttributeDescriptor("programAttributeTypeId", ProgramAttributeType.class));
		return Collections.unmodifiableList(attributeDescriptors);
	}

	@Override
	public String getSubstitution(FormEntrySession session, FormSubmissionController controller, Map<String, String> attributes) {
		ProgramAttributeElement element = new ProgramAttributeElement(session.getContext(), attributes);
		session.getSubmissionController().addAction(element);
		return element.generateHtml(session.getContext());	
	}

}

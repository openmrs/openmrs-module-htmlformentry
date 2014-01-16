package org.openmrs.module.htmlformentry.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.element.DrugOrderSubmissionElement;
import org.springframework.stereotype.Component;
import org.openmrs.annotation.OpenmrsProfile;

/**
 * Handles the {@code <drugOrder>} tag
 */
@Component
@OpenmrsProfile(openmrsVersion = "[1.7.5 - 1.9.*]")
public class DrugOrderTagHandlerSupport1_6 implements DrugOrderTagHandlerSupport {

	@Override
	public List<AttributeDescriptor> createAttributeDescriptors() {
		List<AttributeDescriptor> attributeDescriptors = new ArrayList<AttributeDescriptor>();
		attributeDescriptors.add(new AttributeDescriptor("drugNames", Drug.class));
		attributeDescriptors.add(new AttributeDescriptor("discontinuedReasonConceptId", Concept.class));
		attributeDescriptors.add(new AttributeDescriptor("discontinueReasonAnswers", Concept.class));
		return Collections.unmodifiableList(attributeDescriptors);
	}

	@Override
	public String getSubstitution(FormEntrySession session, FormSubmissionController controllerActions,
								  Map<String, String> parameters) {
		DrugOrderSubmissionElement element = new DrugOrderSubmissionElement(session.getContext(), parameters);
		session.getSubmissionController().addAction(element);

		return element.generateHtml(session.getContext());
	}

}

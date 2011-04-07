package org.openmrs.module.htmlformentry.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.openmrs.Concept;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.element.ObsSubmissionElement;

/**
 * Handles the {@code <obs>} tag
 */
public class ObsTagHandler extends SubstitutionTagHandler {
	
	protected List<AttributeDescriptor> createAttributeDescriptors() {
		List<AttributeDescriptor> attributeDescriptors = new ArrayList<AttributeDescriptor>();
		attributeDescriptors.add(new AttributeDescriptor("conceptId", Concept.class));
		attributeDescriptors.add(new AttributeDescriptor("conceptIds", Concept.class));
		attributeDescriptors.add(new AttributeDescriptor("answerConceptId", Concept.class));
		attributeDescriptors.add(new AttributeDescriptor("answerConceptIds", Concept.class));
		return Collections.unmodifiableList(attributeDescriptors);
	}
	
	@Override
	protected String getSubstitution(FormEntrySession session, FormSubmissionController controllerActions,
	                                 Map<String, String> parameters) {
		ObsSubmissionElement element = new ObsSubmissionElement(session.getContext(), parameters);
		session.getSubmissionController().addAction(element);
		return element.generateHtml(session.getContext());
	}
	
}

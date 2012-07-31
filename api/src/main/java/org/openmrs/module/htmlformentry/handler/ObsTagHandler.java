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
	
	@Override
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
		System.out.println("OBS TAG HANDLER before adding action");		
		session.getSubmissionController().addAction(element);	
		System.out.println("element:"+element);
		String str=element.generateHtml(session.getContext());
		System.out.println(str);
		System.out.println("OBS TAG HANDLER after adding action");
		return str;
	}
	
}

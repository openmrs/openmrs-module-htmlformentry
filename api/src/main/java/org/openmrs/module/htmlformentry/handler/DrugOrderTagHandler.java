package org.openmrs.module.htmlformentry.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.element.DrugOrderSubmissionElement;
import org.openmrs.module.htmlformentry.tag.DrugOrderTag;

/**
 * Handles the {@code <drugOrder>} tag
 */
public class DrugOrderTagHandler extends SubstitutionTagHandler {
	
	@Override
	public List<AttributeDescriptor> createAttributeDescriptors() {
		List<AttributeDescriptor> attributeDescriptors = new ArrayList<AttributeDescriptor>();
		attributeDescriptors.add(new AttributeDescriptor("drugNames", Drug.class));
		attributeDescriptors.add(new AttributeDescriptor("discontinuedReasonConceptId", Concept.class));
		attributeDescriptors.add(new AttributeDescriptor("discontinueReasonAnswers", Concept.class));
		return Collections.unmodifiableList(attributeDescriptors);
	}
	
	@Override
	public String getSubstitution(FormEntrySession session, FormSubmissionController fsc, Map<String, String> parameters) {
		FormEntryContext context = session.getContext();
		DrugOrderTag tag = new DrugOrderTag(parameters);
		DrugOrderSubmissionElement element = new DrugOrderSubmissionElement(context, tag);
		session.getSubmissionController().addAction(element);
		return element.generateHtml(context);
	}
}

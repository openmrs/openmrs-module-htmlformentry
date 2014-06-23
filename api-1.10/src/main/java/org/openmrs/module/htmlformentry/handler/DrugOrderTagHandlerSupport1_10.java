package org.openmrs.module.htmlformentry.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.annotation.OpenmrsProfile;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.element.DrugOrderSubmissionElement1_10;

/**
 * Handles the {@code <drugOrder>} tag
 */
@OpenmrsProfile(openmrsVersion = "1.10")
public class DrugOrderTagHandlerSupport1_10 implements DrugOrderTagHandlerSupport {

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
    	DrugOrderSubmissionElement1_10 element = new DrugOrderSubmissionElement1_10(session.getContext(), parameters);
		session.getSubmissionController().addAction(element);
		
		return element.generateHtml(session.getContext());
    }

}

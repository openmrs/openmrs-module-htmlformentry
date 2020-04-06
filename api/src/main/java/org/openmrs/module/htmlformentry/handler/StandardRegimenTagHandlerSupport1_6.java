package org.openmrs.module.htmlformentry.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.openmrs.Concept;
import org.openmrs.annotation.OpenmrsProfile;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.element.StandardRegimenElement;
import org.openmrs.order.RegimenSuggestion;
import org.springframework.stereotype.Component;

@Component
@OpenmrsProfile(openmrsPlatformVersion = "[1.7.5 - 1.9.*]")
public class StandardRegimenTagHandlerSupport1_6 implements StandardRegimenTagHandlerSupport {
	
	@Override
    public List<AttributeDescriptor> createAttributeDescriptors() {
		List<AttributeDescriptor> attributeDescriptors = new ArrayList<AttributeDescriptor>();
		attributeDescriptors.add(new AttributeDescriptor("discontinuedReasonConceptId", Concept.class));
		attributeDescriptors.add(new AttributeDescriptor("discontinueReasonAnswers", Concept.class));
		attributeDescriptors.add(new AttributeDescriptor("regimenCodes", RegimenSuggestion.class));
		return Collections.unmodifiableList(attributeDescriptors);
	}
	
    @Override
    public String getSubstitution(FormEntrySession session, FormSubmissionController controllerActions,
            Map<String, String> parameters) {
    	
    	StandardRegimenElement element = new StandardRegimenElement(session.getContext(), parameters);
		session.getSubmissionController().addAction(element);
		
		return element.generateHtml(session.getContext());
    }

}

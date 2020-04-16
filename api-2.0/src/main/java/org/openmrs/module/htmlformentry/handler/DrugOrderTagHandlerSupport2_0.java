package org.openmrs.module.htmlformentry.handler;

import java.util.List;
import java.util.Map;

import org.openmrs.annotation.OpenmrsProfile;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.element.DrugOrderSubmissionElement2_0;

/**
 * Handles the {@code <drugOrder>} tag
 */
@OpenmrsProfile(openmrsPlatformVersion = "2.*")
public class DrugOrderTagHandlerSupport2_0 extends DrugOrderTagHandlerSupport1_10 implements DrugOrderTagHandlerSupport{
	
	@Override
    public List<AttributeDescriptor> createAttributeDescriptors() {
		return super.createAttributeDescriptors();
	}
	
    @Override
    public String getSubstitution(FormEntrySession session, FormSubmissionController controllerActions,
            Map<String, String> parameters) {
    	DrugOrderSubmissionElement2_0 element = new DrugOrderSubmissionElement2_0(session.getContext(), parameters);
		session.getSubmissionController().addAction(element);
		
		return element.generateHtml(session.getContext());
    }

}

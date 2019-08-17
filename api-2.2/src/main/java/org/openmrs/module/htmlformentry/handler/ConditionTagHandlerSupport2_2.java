package org.openmrs.module.htmlformentry.handler;

import java.util.Map;

import org.openmrs.annotation.OpenmrsProfile;
import org.openmrs.module.htmlformentry.ConditionElement;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;

@OpenmrsProfile(openmrsPlatformVersion = "2.2.0")
public class ConditionTagHandlerSupport2_2 implements ConditionTagHandlerSupport {
	
	@Override
	public String getSubstitution(FormEntrySession session, FormSubmissionController controller, Map<String, String> attributes) {
		ConditionElement element = new ConditionElement();
		String required = attributes.get("required");
		if (required != null) {
			element.setRequired(required.equalsIgnoreCase("true"));
		}
		session.getSubmissionController().addAction(element);
		return element.generateHtml(session.getContext());	
	}

}

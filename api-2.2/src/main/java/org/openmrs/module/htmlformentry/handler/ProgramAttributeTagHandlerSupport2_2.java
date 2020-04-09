package org.openmrs.module.htmlformentry.handler;

import org.openmrs.annotation.OpenmrsProfile;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.ProgramAttributeElement;

import java.util.Map;

@OpenmrsProfile(openmrsPlatformVersion = "2.2.0")
public class ProgramAttributeTagHandlerSupport2_2 implements ProgramAttributeTagHandlerSupport {
	
	@Override
	public String getSubstitution(FormEntrySession session, FormSubmissionController controller, Map<String, String> attributes) {
		ProgramAttributeElement element = new ProgramAttributeElement(session.getContext(), attributes);
		/*String required = attributes.get("required");
		if (required != null) {
			element.setRequired(required.equalsIgnoreCase("true"));
		}*/
		session.getSubmissionController().addAction(element);
		return element.generateHtml(session.getContext());	
	}

}

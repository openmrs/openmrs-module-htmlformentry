package org.openmrs.module.htmlformentry.handler;

import org.openmrs.annotation.OpenmrsProfile;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;

import java.util.Map;

@OpenmrsProfile(openmrsPlatformVersion = "[1.10 - 2.1.*]")
public class ProgramAttributeTagHandler1_10 implements ProgramAttributeTagHandler {

	@Override
	public String getSubstitution(FormEntrySession session, FormSubmissionController controllerActions,
			Map<String, String> parameters) {
		throw new RuntimeException("The Program Attribute tag should be used on 2.2.0 platform version and above.");
	}

}

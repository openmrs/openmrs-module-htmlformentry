package org.openmrs.module.htmlformentry.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.openmrs.annotation.OpenmrsProfile;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;

/**
 * This is just a place holder to fix HTML-664 because according to TRUNK-14 RegimenSuggestion
 * was removed to be replaced with order sets
 */
@OpenmrsProfile(openmrsPlatformVersion = "2.*")
public class StandardRegimenTagHandlerSupport2_0 implements StandardRegimenTagHandlerSupport {
	
	@Override
	public List<AttributeDescriptor> createAttributeDescriptors() {
		return Collections.unmodifiableList(new ArrayList<AttributeDescriptor>());
	}
	
	@Override
	public String getSubstitution(FormEntrySession session, FormSubmissionController controllerActions,
	        Map<String, String> parameters) {
		return null;
	}
}
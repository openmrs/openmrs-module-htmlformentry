package org.openmrs.module.htmlformentry.handler;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.annotation.OpenmrsProfile;
import org.openmrs.module.htmlformentry.ConditionElement;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;

@OpenmrsProfile(openmrsPlatformVersion = "2.3.1")
public class ConditionTagHandlerSupport2_3 implements ConditionTagHandlerSupport {
	
	private ConditionElement conditionElement;
	
	public ConditionTagHandlerSupport2_3() {
	}
	
	@Override
	public String getSubstitution(FormEntrySession session, FormSubmissionController controller,
	        Map<String, String> attributes) {
		conditionElement = new ConditionElement();
		String required = attributes.get("required");
		if (required != null) {
			conditionElement.setRequired(required.equalsIgnoreCase("true"));
		}
		String formFieldName = attributes.get("formFieldName");
		if (StringUtils.isBlank(formFieldName)) {
			throw new IllegalArgumentException("Parameter formFieldName cannot be blank");
		}
		conditionElement.setFormFieldName(formFieldName);
		session.getSubmissionController().addAction(conditionElement);
		return conditionElement.generateHtml(session.getContext());
	}
	
}

package org.openmrs.module.htmlformentry.handler;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.element.ConditionElement;

public class ConditionTagHandler extends SubstitutionTagHandler {
	
	private ConditionElement conditionElement;
	
	public String getSubstitution(FormEntrySession session, FormSubmissionController controller,
	        Map<String, String> attributes) {
		conditionElement = new ConditionElement();
		
		String required = attributes.get("required");
		if (required != null) {
			conditionElement.setRequired(required.equalsIgnoreCase("true"));
		}
		
		String controlId = attributes.get("controlId");
		if (StringUtils.isBlank(controlId)) {
			throw new IllegalArgumentException(
			        "The condition tage attribute 'controlId' is mandatory and cannot be left blank.");
		}
		conditionElement.setTagControlId(controlId);
		
		String conceptId = attributes.get("conceptId");
		if (!StringUtils.isEmpty(conceptId)) {
			conditionElement.setPresetConcept(HtmlFormEntryUtil.getConcept(conceptId));
		}
		
		String showAdditionalDetail = attributes.get("showAdditionalDetail");
		if (StringUtils.equals("true", showAdditionalDetail)) {
			conditionElement.setAdditionalDetailVisible(true);
		}
		
		session.getSubmissionController().addAction(conditionElement);
		return conditionElement.generateHtml(session.getContext());
	}
}

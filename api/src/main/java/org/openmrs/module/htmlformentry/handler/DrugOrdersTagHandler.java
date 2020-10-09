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
import org.openmrs.module.htmlformentry.element.DrugOrdersSubmissionElement;
import org.openmrs.module.htmlformentry.schema.DrugOrderAnswer;
import org.openmrs.module.htmlformentry.schema.DrugOrderField;
import org.openmrs.module.htmlformentry.tag.TagUtil;

/**
 * Handles the {@code <drugOrders>} tag
 */
public class DrugOrdersTagHandler extends SubstitutionTagHandler {
	
	@Override
	public List<AttributeDescriptor> createAttributeDescriptors() {
		List<AttributeDescriptor> attributeDescriptors = new ArrayList<AttributeDescriptor>();
		attributeDescriptors.add(new AttributeDescriptor("drugs", Drug.class));
		attributeDescriptors.add(new AttributeDescriptor("discontinueReasons", Concept.class));
		return Collections.unmodifiableList(attributeDescriptors);
	}
	
	@Override
	public String getSubstitution(FormEntrySession session, FormSubmissionController fsc, Map<String, String> parameters) {
		FormEntryContext context = session.getContext();
		DrugOrderField field = getDrugOrderField(parameters);
		DrugOrdersSubmissionElement element = new DrugOrdersSubmissionElement(context, parameters, field);
		session.getSubmissionController().addAction(element);
		return element.generateHtml(context);
	}
	
	/**
	 * @return the DrugOrderField represented by this tag
	 */
	public DrugOrderField getDrugOrderField(Map<String, String> parameters) {
		DrugOrderField dof = new DrugOrderField();
		List<Drug> drugs = TagUtil.parseListParameter(parameters, "drugs", Drug.class);
		List<String> drugNames = TagUtil.parseListParameter(parameters, "drugLabels", String.class);
		for (int i = 0; i < drugs.size(); i++) {
			Drug drug = drugs.get(i);
			String drugName = (i < drugNames.size() ? drugNames.get(i) : drug.getDisplayName());
			dof.addDrugOrderAnswer(new DrugOrderAnswer(drug, drugName));
		}
		return dof;
	}
}

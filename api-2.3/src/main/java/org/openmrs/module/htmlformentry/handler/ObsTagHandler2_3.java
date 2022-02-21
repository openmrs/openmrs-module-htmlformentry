package org.openmrs.module.htmlformentry.handler;

import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.LocationTag;
import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.FormEntryContext2_3;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.HtmlFormEntryConstants;
import org.openmrs.module.htmlformentry.element.ObsSubmissionElement2_3;
import org.w3c.dom.Node;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ObsTagHandler2_3 extends ObsTagHandler {
	
	@Override
	public boolean doStartTag(FormEntrySession session, PrintWriter out, Node parent, Node node)
	        throws BadFormDesignException {
		FormEntryContext2_3 context = (FormEntryContext2_3) session.getContext();
		ObsSubmissionElement2_3 element = new ObsSubmissionElement2_3(context, getAttributes(node));
		session.getSubmissionController().addAction(element);
		out.print(element.generateHtml(context));
		
		context.pushToStack(element);
		return true;
	}
	
	@Override
	protected List<AttributeDescriptor> createAttributeDescriptors() {
		List<AttributeDescriptor> attributeDescriptors = new ArrayList<AttributeDescriptor>();
		attributeDescriptors.add(new AttributeDescriptor("conceptId", Concept.class));
		attributeDescriptors.add(new AttributeDescriptor("conceptIds", Concept.class));
		attributeDescriptors.add(new AttributeDescriptor("answerConceptId", Concept.class));
		attributeDescriptors.add(new AttributeDescriptor("answerDrugId", Drug.class));
		attributeDescriptors.add(new AttributeDescriptor("answerConceptIds", Concept.class));
		attributeDescriptors.add(new AttributeDescriptor("controlId", Concept.class));
		attributeDescriptors.add(new AttributeDescriptor(HtmlFormEntryConstants.ANSWER_LOCATION_TAGS, LocationTag.class));
		return Collections.unmodifiableList(attributeDescriptors);
	}
	
}

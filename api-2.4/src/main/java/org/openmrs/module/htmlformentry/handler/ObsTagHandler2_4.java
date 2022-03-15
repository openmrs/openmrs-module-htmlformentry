package org.openmrs.module.htmlformentry.handler;

import org.openmrs.Concept;
import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.FormEntryContext2_4;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.element.ObsSubmissionElement2_3;
import org.w3c.dom.Node;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ObsTagHandler2_4 extends ObsTagHandler {
	
	@Override
	public boolean doStartTag(FormEntrySession session, PrintWriter out, Node parent, Node node)
	        throws BadFormDesignException {
		FormEntryContext2_4 context = (FormEntryContext2_4) session.getContext();
		ObsSubmissionElement2_3 element = new ObsSubmissionElement2_3(context, getAttributes(node));
		session.getSubmissionController().addAction(element);
		out.print(element.generateHtml(context));
		
		context.pushToStack(element);
		return true;
	}
	
	@Override
	protected List<AttributeDescriptor> createAttributeDescriptors() {
		List<AttributeDescriptor> attributeDescriptors = new ArrayList<>(super.createAttributeDescriptors());
		attributeDescriptors.add(new AttributeDescriptor("controlId", Concept.class));
		return Collections.unmodifiableList(attributeDescriptors);
	}
	
}

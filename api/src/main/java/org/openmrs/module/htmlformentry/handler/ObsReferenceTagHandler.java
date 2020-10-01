package org.openmrs.module.htmlformentry.handler;

import java.io.PrintWriter;

import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.element.ObsReferenceSubmissionElement;
import org.w3c.dom.Node;

public class ObsReferenceTagHandler extends ObsTagHandler {
	
	@Override
	public boolean doStartTag(FormEntrySession session, PrintWriter out, Node parent, Node node)
	        throws BadFormDesignException {
		FormEntryContext context = session.getContext();
		ObsReferenceSubmissionElement element = new ObsReferenceSubmissionElement(context, getAttributes(node));
		session.getSubmissionController().addAction(element);
		out.print(element.generateHtml(context));
		context.pushToStack(element);
		return true;
	}
	
}

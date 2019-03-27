package org.openmrs.module.htmlformentry.handler;

import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.element.ObsSubmissionElement;
import org.openmrs.module.htmlformentry.element.VisitObsSubmissionElement;
import org.w3c.dom.Node;

import java.io.PrintWriter;

/**
 * Handles the {@code <obs>} tag
 */
public class VisitObsTagHandler extends ObsTagHandler {

    @Override
    public boolean doStartTag(FormEntrySession session, PrintWriter out, Node parent, Node node) throws BadFormDesignException {
        FormEntryContext context = session.getContext();
        VisitObsSubmissionElement element = new VisitObsSubmissionElement(context, getAttributes(node));

        session.getSubmissionController().addAction(element);
        out.print(element.generateHtml(context));

        context.pushToStack(element);
        return true;
    }
}

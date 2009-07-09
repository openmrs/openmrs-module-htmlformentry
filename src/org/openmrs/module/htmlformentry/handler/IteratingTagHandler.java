package org.openmrs.module.htmlformentry.handler;

import java.io.PrintWriter;

import org.openmrs.module.htmlformentry.FormEntrySession;
import org.w3c.dom.Node;

public interface IteratingTagHandler extends TagHandler {

    public boolean shouldRunAgain(FormEntrySession session, PrintWriter out, Node parent, Node node);
    
}

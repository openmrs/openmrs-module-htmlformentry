package org.openmrs.module.htmlformentry.handler;

import java.io.PrintWriter;

import org.openmrs.module.htmlformentry.FormEntrySession;
import org.w3c.dom.Node;

/**
 * Implementations of this interface know how to handle a specific htmlform tag like {@code <obs/>} or {@code <encounterDatetime/>}.
 */
public interface TagHandler {

    /**
     * Handles the start tag for a specific tag type. Generates the appropriate HTML and adds it to the associated PrintWriter.
     * Also adds any necessary FormSubmissionControllerActions to the FormSubmissionController associated with the session.
     * Returns whether or not to handle the body also. (True = Yes)
     * 
     * @param session the current session
     * @param out the PrintWriter to append generated HTML to
     * @param parent the parent node of the node in the XML associated with this tag
     * @param node the node in the XML associated with this tag
     * @return true/false whether to handle the body
     */
    public boolean doStartTag(FormEntrySession session, PrintWriter out, Node parent, Node node);
        
    /**
     * Handles the end tag for a specific tag type.  Generates the approriate HTML and adds it to the associated PrintWriter.
     * Also adds any necessary FormSubmissionControllerActions to the FormSubmissionController associated with the session.
     * 
     * @param session the current session
     * @param out the PrintWriter to append generated HTML to
     * @param parent the parent node of the node in the XML associated with this tag
     * @param node the node in the XML associated with this tag
     */
    public void doEndTag(FormEntrySession session, PrintWriter out, Node parent, Node node);
    
}

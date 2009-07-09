package org.openmrs.module.htmlformentry.handler;

import java.io.PrintWriter;

import org.openmrs.module.htmlformentry.FormEntrySession;
import org.w3c.dom.Node;

/**
 * Implementations of this interface know how to handle htmlform tags like <obs/> or <encounterDatetime/>.
 */
public interface TagHandler {

    /**
     * Returns whether or not to handle the body also. (True = Yes)
     * 
     * @param session
     * @param out
     * @param parent
     * @param node
     * @return
     */
    public boolean doStartTag(FormEntrySession session, PrintWriter out, Node parent, Node node);
        
    public void doEndTag(FormEntrySession session, PrintWriter out, Node parent, Node node);
    
}

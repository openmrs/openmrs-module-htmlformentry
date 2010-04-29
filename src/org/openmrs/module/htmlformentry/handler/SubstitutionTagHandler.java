package org.openmrs.module.htmlformentry.handler;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * An abstract class that provides convenient way to implement a tag handler than just replaces the tag with a dynamically generated string.
 * (For example {@code <encounterDate/>} gets substituted with appropriate Date widget.)
 * 
 * Just override the getSubstitution() method.
 */
public abstract class SubstitutionTagHandler implements TagHandler {

	/** The logger to use with this class */
    protected final Log log = LogFactory.getLog(getClass());

    /**
     * Generates and returns the HTML to substitute for a specific tag
     * 
     * @param session the current session
     * @param controllerActions the FormSubmissionController associated with the session
     * @param parameters any parameters associated with the tag
     * @return
     */
    abstract protected String getSubstitution(FormEntrySession session, FormSubmissionController controllerActions, Map<String, String> parameters);
    
    public boolean doStartTag(FormEntrySession session, PrintWriter out, Node parent, Node node) {
        Map<String, String> attributes = new HashMap<String, String>();        
        NamedNodeMap map = node.getAttributes();
        for (int i = 0; i < map.getLength(); ++i) {
            Node attribute = map.item(i);
            attributes.put(attribute.getNodeName(), attribute.getNodeValue());
        }
        
        String replacement = getSubstitution(session, session.getSubmissionController(), attributes);
        out.print(replacement);
        return false; // skip contents/children
    }

    public void doEndTag(FormEntrySession session, PrintWriter out, Node parent, Node node) {
        // do nothing
    }

}

package org.openmrs.module.htmlformentry.handler;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.w3c.dom.Node;

import java.io.PrintWriter;
import java.util.Map;

/**
 * An abstract class that provides convenient way to implement a tag handler than just replaces the tag with a dynamically generated string.
 * (For example {@code <encounterDate/>} gets substituted with appropriate Date widget.)
 * 
 * Just override the getSubstitution() method.
 */
public abstract class SubstitutionTagHandler extends AbstractTagHandler {

	/** The logger to use with this class */
    protected final Log log = LogFactory.getLog(getClass());
    
    /**
     * Generates and returns the HTML to substitute for a specific tag
     * 
     * @param session the current session
     * @param controllerActions the FormSubmissionController associated with the session
     * @param parameters any parameters associated with the tag
     * @return
     * @throws BadFormDesignException 
     */
    abstract protected String getSubstitution(FormEntrySession session, FormSubmissionController controllerActions, Map<String, String> parameters) throws BadFormDesignException;
    
    @Override
    public boolean doStartTag(FormEntrySession session, PrintWriter out, Node parent, Node node) throws BadFormDesignException {

        String replacement = getSubstitution(session, session.getSubmissionController(), getAttributes(node));
        out.print(replacement);
        return false; // skip contents/children
    }

    @Override
    public void doEndTag(FormEntrySession session, PrintWriter out, Node parent, Node node) {
        // do nothing
    }

    protected boolean parseBooleanAttribute(String val, boolean defaultValue) {
        if (StringUtils.isEmpty(val)) {
            return defaultValue;
        }
        else {
            return Boolean.parseBoolean(val);
        }
    }

}

package org.openmrs.module.htmlformentry.handler;

import java.io.PrintWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.Translator;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Handles the {@code <designerWidget>} tag
 * Actually, it only specifies children nodes should be handled
 */
public class DesignerWidgetHandler extends AbstractTagHandler {

	/** The logger to use with this class */
    protected final Log log = LogFactory.getLog(getClass());
    
    public boolean doStartTag(FormEntrySession session, PrintWriter out, Node parent, Node node) {
        //ignores... children nodes should be handled
        return true;
    }

    public void doEndTag(FormEntrySession session, PrintWriter out, Node parent, Node node) {
    	
    }
    
    @Override
    public boolean allowsChildren() {
    	return true;
    }

}

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
 * Handles the {@code <section>} tag
 */
public class SectionTagHandler implements TagHandler {

	/** The logger to use with this class */
    protected final Log log = LogFactory.getLog(getClass());
    
    public boolean doStartTag(FormEntrySession session, PrintWriter out, Node parent, Node node) {
        String sectionStyleClass = "section";
        String headerStyleClass = "sectionHeader";
        String headerLabel = null;
        NamedNodeMap map = node.getAttributes();
        for (int i = 0; i < map.getLength(); ++i) {
            Node attribute = map.item(i);
            if (attribute.getNodeName().equals("sectionStyle")) {
            	sectionStyleClass = attribute.getNodeValue();
            }
            if (attribute.getNodeName().equals("headerStyle")) {
            	headerStyleClass = attribute.getNodeValue();
            }
            if (attribute.getNodeName().equals("headerLabel")) {
            	headerLabel = attribute.getNodeValue();
            }
            if (attribute.getNodeName().equals("headerCode")) {
            	Translator trans = session.getContext().getTranslator();
            	headerLabel = trans.translate(Context.getLocale().toString(), attribute.getNodeValue());
            }
        }
        out.print("<div class=\""+sectionStyleClass+"\">");
        if (headerLabel != null) {
        	out.print("<span class=\""+headerStyleClass+"\">"+headerLabel+"</span>");
        }
        
        session.getContext().getSchema().startNewSection();
        session.getContext().getSchema().getLastSection().setName(headerLabel);
        
        return true;
    }

    public void doEndTag(FormEntrySession session, PrintWriter out, Node parent, Node node) {
    	out.print("</div>");
    }

}

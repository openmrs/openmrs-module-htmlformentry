package org.openmrs.module.htmlformentry.handler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.Translator;
import org.w3c.dom.Node;

import java.io.PrintWriter;

/**
 * Handles the {@code <section>} tag
 */
public class SectionTagHandler extends AbstractTagHandler {

	/** The logger to use with this class */
    protected final Log log = LogFactory.getLog(getClass());
    
    @Override
    public boolean doStartTag(FormEntrySession session, PrintWriter out, Node parent, Node node) {
        String sectionTag = getAttribute(node, "sectionTag", "div");
        String headerTag = getAttribute(node, "headerTag", "span");
        String sectionStyleClass = getAttribute(node, "sectionStyle", "section");
        String headerStyleClass = getAttribute(node, "headerStyle", "sectionHeader");
        String sectionId = getAttribute(node, "id", null);

        String headerLabel = getAttribute(node, "headerLabel", null);
        if (headerLabel == null) {
            String headerCode = getAttribute(node, "headerCode", null);
            if (headerCode != null) {
                Translator trans = session.getContext().getTranslator();
                headerLabel = trans.translate(Context.getLocale().toString(), headerCode);
            }
        }

        out.print("<" + sectionTag + " class=\""+sectionStyleClass + "\"");
        if (sectionId != null) {
            out.print(" id=\"" + sectionId + "\"");
        }
        out.print(">");
        if (headerLabel != null) {
        	out.print("<" + headerTag + " class=\""+headerStyleClass+"\">"+headerLabel+"</" + headerTag + ">");
        }
        
        session.getContext().beginSection();
        session.getContext().getActiveSection().setName(headerLabel);
        
        return true;
    }

    @Override
    public void doEndTag(FormEntrySession session, PrintWriter out, Node parent, Node node) {
        String sectionTag = getAttribute(node, "sectionTag", "div");
    	out.print("</" + sectionTag + ">");
    	session.getContext().endSection();
    }

}

package org.openmrs.module.htmlformentry.handler;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.element.PatientElement;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Currently not used? Meant to handles the {@code <patient>} tag?
 */
public class PatientTagHandler implements TagHandler {
	
	public boolean doStartTag(FormEntrySession session, PrintWriter out, Node parent, Node node) {
		Map<String, String> attributes = new HashMap<String, String>();        
        NamedNodeMap map = node.getAttributes();
        for (int i = 0; i < map.getLength(); ++i) {
            Node attribute = map.item(i);
            attributes.put(attribute.getNodeName(), attribute.getNodeValue());
        }
        
        try {
        	Boolean.parseBoolean(attributes.get("allowCreate"));
        } catch (Exception ex) {
        	attributes.put("allowCreate", "true");
        }
        try {
        	Boolean.parseBoolean(attributes.get("showWhenExists"));
        } catch (Exception ex) {
        	attributes.put("showWhenExists", "false");
        }
        
        PatientElement element = new PatientElement(session.getContext(), attributes);
        session.getSubmissionController().addAction(element);
        out.print(element.generateHtml(session.getContext()));
        return true;
	}
	
	public void doEndTag(FormEntrySession session, PrintWriter out, Node parent, Node node) {
		// TODO Auto-generated method stub
		
	}
	
}

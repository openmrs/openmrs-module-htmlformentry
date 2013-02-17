package org.openmrs.module.htmlformentry.handler;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.action.RepeatControllerAction;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Currently not used--the {@code <repeat>} tag is currently handled by {@see org.openmrs.module.htmlformentry.HtmlFormEntryGenerator#applyRepeats(String)}.
 */
public abstract class RepeatTagHandler implements IteratingTagHandler {

    @Override
    public boolean doStartTag(FormEntrySession session, PrintWriter out, Node parent, Node node) {
        Map<String, String> attributes = new HashMap<String, String>();
        NamedNodeMap map = node.getAttributes();
        for (int i = 0; i < map.getLength(); ++i) {
            Node attribute = map.item(i);
            attributes.put(attribute.getNodeName(), attribute.getNodeValue());
        }
        
        setupBefore(session, attributes);
        session.getSubmissionController().startRepeat(getRepeatAction(session, attributes));
        return true; // recurse to children
    }

    @Override
    public void doEndTag(FormEntrySession session, PrintWriter out, Node parent, Node node) {
        session.getSubmissionController().endRepeat();
    }

    protected abstract void setupBefore(FormEntrySession session, Map<String, String> attributes);
    
    protected abstract RepeatControllerAction getRepeatAction(FormEntrySession session, Map<String, String> attributes);

}

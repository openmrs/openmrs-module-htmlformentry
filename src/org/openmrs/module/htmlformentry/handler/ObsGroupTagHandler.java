package org.openmrs.module.htmlformentry.handler;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.ObsGroupComponent;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.action.ObsGroupAction;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Handles the {@code <obsGroup>} tag
 */
public class ObsGroupTagHandler implements TagHandler {
    
    /**
     * @see org.openmrs.module.htmlformentry.handler.TagHandler#doStartTag(org.openmrs.module.htmlformentry.FormEntrySession, java.io.PrintWriter, org.w3c.dom.Node, org.w3c.dom.Node)
     */
    public boolean doStartTag(FormEntrySession session, PrintWriter out,
            Node parent, Node node) {
        
        Map<String, String> attributes = new HashMap<String, String>();        
        NamedNodeMap map = node.getAttributes();
        for (int i = 0; i < map.getLength(); ++i) {
            Node attribute = map.item(i);
            attributes.put(attribute.getNodeName(), attribute.getNodeValue());
        }
        if (attributes.get("groupingConceptId") == null) {
            throw new NullPointerException("obsgroup tag requires a groupingConceptId attribute");
        }
        Concept groupingConcept = Context.getConceptService().getConcept(Integer.valueOf(attributes.get("groupingConceptId")));
        if (groupingConcept == null) {
            throw new NullPointerException("could not find concept " + attributes.get("groupingConceptId") + " as grouping obs for an obsgroup tag");
        }
                    
        // avoid lazy init exception
        groupingConcept.getDatatype().getHl7Abbreviation();
                
        // find relevant obs group to display for this element
        Obs thisGroup = findObsGroup(session, parent, node);
        
        session.getContext().beginObsGroup(groupingConcept);
        session.getContext().setObsGroup(thisGroup);
        
        session.getSubmissionController().addAction(ObsGroupAction.start(groupingConcept, thisGroup));
        return true;
    }

    private Obs findObsGroup(FormEntrySession session, Node parent, Node node) {
    	Concept groupingConcept = Context.getConceptService().getConcept(Integer.valueOf(node.getAttributes().getNamedItem("groupingConceptId").getNodeValue()));
        List<ObsGroupComponent> questionsAndAnswers = findQuestionsAndAnswersForGroup(node);
        return session.getContext().findFirstMatchingObsGroup(groupingConcept,questionsAndAnswers);
    }

    private List<ObsGroupComponent> findQuestionsAndAnswersForGroup(Node node) {
        List<ObsGroupComponent> ret = new ArrayList<ObsGroupComponent>();
        findQuestionsAndAnswersForGroupHelper(node, ret);
        return ret;
    }
    

    private void findQuestionsAndAnswersForGroupHelper(Node node, List<ObsGroupComponent> ret) {
        if ("obs".equals(node.getNodeName())) {
            Concept question = null;
            Concept answer = null;
            NamedNodeMap attrs = node.getAttributes();
            try {
                question = Context.getConceptService().getConcept(Integer.valueOf(attrs.getNamedItem("conceptId").getNodeValue()));
            } catch (Exception ex) {
                throw new RuntimeException("Error getting conceptId from obs in obsgroup", ex);
            }
            try {
                answer = Context.getConceptService().getConcept(Integer.valueOf(attrs.getNamedItem("answerConceptId").getNodeValue()));
            } catch (Exception ex) {
                // this is fine
            }
            ret.add(new ObsGroupComponent(question, answer));
        } else {
            NodeList nl = node.getChildNodes();
            for (int i = 0; i < nl.getLength(); ++i) {
                findQuestionsAndAnswersForGroupHelper(nl.item(i), ret);
            }
        }
    }

    public void doEndTag(FormEntrySession session, PrintWriter out, Node parent, Node node) {
        if(session.getContext().getMode()==Mode.VIEW) out.print("<br/>");
    	session.getContext().endObsGroup();
        session.getContext().setObsGroup(null);
        session.getSubmissionController().addAction(ObsGroupAction.end());
    }
    
}

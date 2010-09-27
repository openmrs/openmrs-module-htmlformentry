package org.openmrs.module.htmlformentry.handler;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.ObsGroupComponent;
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
        Concept groupingConcept = HtmlFormEntryUtil.getConcept(attributes.get("groupingConceptId"));
        if (groupingConcept == null) {
            throw new NullPointerException("could not find concept " + attributes.get("groupingConceptId") + " as grouping obs for an obsgroup tag");
        }
                    
        // avoid lazy init exception
        groupingConcept.getDatatype().getHl7Abbreviation();
                
        // find relevant obs group to display for this element
        Obs thisGroup = findObsGroup(session, node, attributes.get("groupingConceptId"));
        // sets the current obs group members in the context
        session.getContext().setObsGroup(thisGroup);
        // sets up the obs group stack, sets current obs group to this one
        session.getContext().beginObsGroup(groupingConcept);
        
        //adds the obsgroup action to the controller stack
        session.getSubmissionController().addAction(ObsGroupAction.start(groupingConcept, thisGroup));
        return true;
    }

    private Obs findObsGroup(FormEntrySession session, Node node, String parentGroupingConceptId) {
        List<ObsGroupComponent> questionsAndAnswers = ObsGroupComponent.findQuestionsAndAnswersForGroup(parentGroupingConceptId, node);
        int depth = getObsGroupDepth(node);
        return session.getContext().findBestMatchingObsGroup(questionsAndAnswers, parentGroupingConceptId, depth);
    }
    
    /**
     * 
     * Gets hierarchy level of obsgroup tag.  obsgroups at top level of encounter returns 1.  obsgroups inside of top level obs groups return 2.  etc...
     * 
     * @param node
     * @return
     */
    private int getObsGroupDepth(Node node){
        int obsGroupDepth = 1;
        Node tmpNode = node;
        while (!tmpNode.getNodeName().equals("htmlform")){
            tmpNode = tmpNode.getParentNode();
            if (tmpNode.getNodeName().equals("obsgroup"))
                obsGroupDepth++;
        }
        return obsGroupDepth;
    }

    public void doEndTag(FormEntrySession session, PrintWriter out, Node parent, Node node) {
//                Concept question = null;
//                if (parent != null){
//                    NamedNodeMap attrs = parent.getAttributes();
//                    try {
//                        question = Context.getConceptService().getConcept(Integer.valueOf(attrs.getNamedItem("groupingConceptId").getNodeValue()));
//                    } catch (Exception ex){}    
//                }
                 session.getContext().endObsGroup();
                 session.getSubmissionController().addAction(ObsGroupAction.end());
    }
    
}

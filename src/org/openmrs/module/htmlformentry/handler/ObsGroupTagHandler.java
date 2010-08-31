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
        Obs thisGroup = findObsGroup(session, parent, node, attributes.get("groupingConceptId"));
        session.getContext().beginObsGroup(groupingConcept);
        session.getContext().setObsGroup(thisGroup);
        
        session.getSubmissionController().addAction(ObsGroupAction.start(groupingConcept, thisGroup));
        return true;
    }

    private Obs findObsGroup(FormEntrySession session, Node parent, Node node, String parentGroupingConceptId) {
        List<ObsGroupComponent> questionsAndAnswers = findQuestionsAndAnswersForGroup(parentGroupingConceptId, node);
        return session.getContext().findFirstMatchingObsGroup(questionsAndAnswers);
    }


    private List<ObsGroupComponent> findQuestionsAndAnswersForGroup(String parentGroupingConceptId, Node node) {
        List<ObsGroupComponent> ret = new ArrayList<ObsGroupComponent>();
        findQuestionsAndAnswersForGroupHelper(parentGroupingConceptId,node, ret);
        return ret;
    }
    

    private void findQuestionsAndAnswersForGroupHelper(String parentGroupingConceptId, Node node, List<ObsGroupComponent> ret) {
        if ("obs".equals(node.getNodeName())) {
            Concept question = null;
            Concept answer = null;
            List<Concept> answersList = null;
            NamedNodeMap attrs = node.getAttributes();
            try {
                question = HtmlFormEntryUtil.getConcept(attrs.getNamedItem("conceptId").getNodeValue());
            } catch (Exception ex) {
                throw new RuntimeException("Error getting conceptId from obs in obsgroup", ex);
            }
            try {
                answer = HtmlFormEntryUtil.getConcept(attrs.getNamedItem("answerConceptId").getNodeValue());
            } catch (Exception ex) {
                // this is fine
            }
            //check for answerConceptIds (plural)
            if (answer == null){
                Node n = attrs.getNamedItem("answerConceptIds");
                if (n != null){
                    String answersIds = n.getNodeValue();
                    if (answersIds != null && !answersIds.equals("")){
                        //initialize list
                        answersList = new ArrayList<Concept>();
                        for (StringTokenizer st = new StringTokenizer(answersIds, ","); st.hasMoreTokens(); ) {
                            try {
                                answersList.add(HtmlFormEntryUtil.getConcept(st.nextToken().trim()));
                            } catch (Exception ex){
                                //just ignore invalid conceptId if encountered in answersList
                            }
                        } 
                    }
                }
            }
           
          //deterimine whether or not the obs group parent of this obs is the obsGroup obs that we're looking at.
            boolean thisObsInThisGroup = false;
            Node pTmp = node.getParentNode();
            while(pTmp.getParentNode() != null){
                          
                Map<String, String> attributes = new HashMap<String, String>();        
                NamedNodeMap map = pTmp.getAttributes();
                if (map != null)
                    for (int i = 0; i < map.getLength(); ++i) {
                        Node attribute = map.item(i);
                        attributes.put(attribute.getNodeName(), attribute.getNodeValue());
                    }
                if (attributes.containsKey("groupingConceptId")){
                    if (attributes.get("groupingConceptId").equals(parentGroupingConceptId)){
                        thisObsInThisGroup = true;
                        break;
                    } else {
                        break;
                    }
                } 
                pTmp = pTmp.getParentNode();
            }
            if (thisObsInThisGroup){
                if (answersList != null && answersList.size() > 0)
                    for (Concept c : answersList)
                        ret.add(new ObsGroupComponent(question, c));
                else
                    ret.add(new ObsGroupComponent(question, answer));   
            }    
        } else {
             NodeList nl = node.getChildNodes();
             for (int i = 0; i < nl.getLength(); ++i) {
                 findQuestionsAndAnswersForGroupHelper(parentGroupingConceptId, nl.item(i), ret);
             }
         }
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
                 session.getContext().setObsGroup(null);
                 session.getSubmissionController().addAction(ObsGroupAction.end());
    }
    
}

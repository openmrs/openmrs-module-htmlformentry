package org.openmrs.module.htmlformentry.handler;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.ObsGroupComponent;
import org.openmrs.module.htmlformentry.action.ObsGroupAction;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Handles the {@code <obsGroup>} tag
 */
public class ObsGroupTagHandler extends AbstractTagHandler {
   
	protected List<AttributeDescriptor> createAttributeDescriptors() {
		List<AttributeDescriptor> attributeDescriptors = new ArrayList<AttributeDescriptor>();	
		attributeDescriptors.add(new AttributeDescriptor("groupingConceptId", Concept.class));
		return Collections.unmodifiableList(attributeDescriptors);
	}
	
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
        // sets up the obs group stack, sets current obs group to this one
        session.getContext().beginObsGroup(groupingConcept, thisGroup);
        //adds the obsgroup action to the controller stack
        session.getSubmissionController().addAction(ObsGroupAction.start(groupingConcept, thisGroup));
        return true;
    }

    private Obs findObsGroup(FormEntrySession session, Node node, String parentGroupingConceptId) {
        List<ObsGroupComponent> questionsAndAnswers = ObsGroupComponent.findQuestionsAndAnswersForGroup(parentGroupingConceptId, node);
        String path = ObsGroupComponent.getObsGroupPath(node);
        return session.getContext().findBestMatchingObsGroup(questionsAndAnswers, parentGroupingConceptId, path);
    }

    public void doEndTag(FormEntrySession session, PrintWriter out, Node parent, Node node) {
//                Concept question = null;
//                if (parent != null){
//                    NamedNodeMap attrs = parent.getAttributes();
//                    try {
//                        question = HtmlFormEntryUtil.getConcept(attrs.getNamedItem("groupingConceptId").getNodeValue());
//                    } catch (Exception ex){}    
//                }
                 session.getContext().endObsGroup();
                 session.getSubmissionController().addAction(ObsGroupAction.end());
    }
    
}

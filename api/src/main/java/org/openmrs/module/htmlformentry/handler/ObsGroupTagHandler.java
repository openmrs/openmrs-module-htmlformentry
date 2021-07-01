package org.openmrs.module.htmlformentry.handler;

import java.io.PrintWriter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.ObsGroupComponent;
import org.openmrs.module.htmlformentry.action.ObsGroupAction;
import org.openmrs.module.htmlformentry.matching.ObsGroupEntity;
import org.openmrs.module.htmlformentry.schema.ObsGroup;
import org.w3c.dom.Node;

/**
 * Handles the {@code <obsGroup>} tag
 */
public class ObsGroupTagHandler extends AbstractTagHandler {
	
	boolean unmatchedInd = false;
	
	ObsGroup ogSchemaObj;
	
	@Override
	protected List<AttributeDescriptor> createAttributeDescriptors() {
		List<AttributeDescriptor> attributeDescriptors = new ArrayList<AttributeDescriptor>();
		attributeDescriptors.add(new AttributeDescriptor("groupingConceptId", Concept.class));
		attributeDescriptors.add(new AttributeDescriptor("hiddenConceptId", Concept.class));
		attributeDescriptors.add(new AttributeDescriptor("hiddenAnswerConceptId", Concept.class));
		return Collections.unmodifiableList(attributeDescriptors);
	}
	
	/**
	 * @see org.openmrs.module.htmlformentry.handler.TagHandler#doStartTag(org.openmrs.module.htmlformentry.FormEntrySession,
	 *      java.io.PrintWriter, org.w3c.dom.Node, org.w3c.dom.Node)
	 */
	@Override
	public boolean doStartTag(FormEntrySession session, PrintWriter out, Node parent, Node node)
	        throws BadFormDesignException {
		
		Map<String, String> attributes = getAttributes(node);
		if (attributes.get("groupingConceptId") == null) {
			throw new BadFormDesignException("obsgroup tag requires a groupingConceptId attribute");
		}
		Concept groupingConcept = HtmlFormEntryUtil.getConcept(attributes.get("groupingConceptId"));
		if (groupingConcept == null) {
			throw new BadFormDesignException("could not find concept " + attributes.get("groupingConceptId")
			        + " as grouping obs for an obsgroup tag");
		}
		
		boolean ignoreIfEmpty = session.getContext().getMode() == Mode.VIEW && "false".equals(attributes.get("showIfEmpty"));
		
		// avoid lazy init exception
		groupingConcept.getDatatype().getHl7Abbreviation();
		
		String name = attributes.get("label");
		// find relevant obs group to display for this element
		Obs thisGroup = findObsGroup(session, node, attributes.get("groupingConceptId"));
		
		boolean digDeeper = true;
		
		if (thisGroup == null
		        && (session.getContext().getMode() == Mode.EDIT || session.getContext().getMode() == Mode.VIEW)) {
			if (!session.getContext().isUnmatchedMode()) {
				unmatchedInd = true;
				
				ObsGroupEntity obsGroupEntity = new ObsGroupEntity();
				obsGroupEntity.setPath(ObsGroupComponent.getObsGroupPath(node));
				obsGroupEntity.setQuestionsAndAnswers(
				    ObsGroupComponent.findQuestionsAndAnswersForGroup(attributes.get("groupingConceptId"), node));
				obsGroupEntity.setXmlObsGroupConcept(attributes.get("groupingConceptId"));
				obsGroupEntity.setGroupingConcept(groupingConcept);
				obsGroupEntity.setNode(node);
				int unmatchedObsGroupId = session.getContext().addUnmatchedObsGroupEntities(obsGroupEntity);
				out.print(String.format("<unmatched id=\"%s\" />", unmatchedObsGroupId));
				digDeeper = false;
			}
		} else {
			unmatchedInd = false;
		}
		
		if (ignoreIfEmpty && thisGroup == null) {
			digDeeper = false;
		}
		
		// sets up the obs group stack, sets current obs group to this one
		ogSchemaObj = new ObsGroup(groupingConcept, name);
		ogSchemaObj.sethiddenObs(gethiddenObs(attributes.get("hiddenConceptId"), attributes.get("hiddenAnswerConceptId")));
		session.getContext().beginObsGroup(groupingConcept, thisGroup, ogSchemaObj);
		//adds the obsgroup action to the controller stack
		session.getSubmissionController().addAction(ObsGroupAction.start(groupingConcept, thisGroup, ogSchemaObj));
		return digDeeper;
	}
	
	private Obs findObsGroup(FormEntrySession session, Node node, String parentGroupingConceptId) {
		String path = ObsGroupComponent.getObsGroupPath(node);
		
		if (session.getContext().isUnmatchedMode()) {
			return session.getContext().getNextUnmatchedObsGroup(path);
		} else {
			List<ObsGroupComponent> questionsAndAnswers = ObsGroupComponent
			        .findQuestionsAndAnswersForGroup(parentGroupingConceptId, node);
			return session.getContext().findBestMatchingObsGroup(questionsAndAnswers, parentGroupingConceptId, path);
		}
		
	}
	
	private Map.Entry<Concept, Concept> gethiddenObs(String hiddenConceptId, String hiddenAnswerConceptId)
	        throws BadFormDesignException {
		if (hiddenConceptId != null || hiddenAnswerConceptId != null) {
			if (hiddenConceptId == null || hiddenAnswerConceptId == null) {
				throw new BadFormDesignException(
				        "obsgroup tags 'hiddenConceptId' and 'hiddenAnswerConceptId' must be used together");
			}
			Concept question = HtmlFormEntryUtil.getConcept(hiddenConceptId);
			if (question == null) {
				throw new BadFormDesignException(
				        "could not find concept " + hiddenConceptId + " as the hiddenConceptId for an obsgroup tag");
			}
			Concept answer = HtmlFormEntryUtil.getConcept(hiddenAnswerConceptId);
			if (answer == null) {
				throw new BadFormDesignException("could not find concept " + hiddenAnswerConceptId
				        + " as the hiddenAnswerConceptId for an obsgroup tag");
			}
			return new AbstractMap.SimpleEntry<>(question, answer);
		} else {
			return null;
		}
	}
	
	@Override
	public void doEndTag(FormEntrySession session, PrintWriter out, Node parent, Node node) {
		//                Concept question = null;
		//                if (parent != null){
		//                    NamedNodeMap attrs = parent.getAttributes();
		//                    try {
		//                        question = HtmlFormEntryUtil.getConcept(attrs.getNamedItem("groupingConceptId").getNodeValue());
		//                    } catch (Exception ex){}    
		//                }
		session.getContext().endObsGroup();
		session.getSubmissionController().addAction(ObsGroupAction.end(ogSchemaObj));
	}
	
	@Override
	public TagAnalysis validate(Node node) {
		TagAnalysis analysis = new TagAnalysis();
		String groupingConceptId = HtmlFormEntryUtil.getNodeAttribute(node, "groupingConceptId", null);
		if (groupingConceptId == null) {
			analysis.addError(Context.getMessageSourceService().getMessage("htmlformentry.error.groupingConceptIdMissing"));
		} else {
			Concept groupingConcept = HtmlFormEntryUtil.getConcept(groupingConceptId);
			if (groupingConcept == null) {
				analysis.addError(Context.getMessageSourceService().getMessage("htmlformentry.error.invalidConcept",
				    new Object[] { groupingConceptId }, null));
			} else {
				if (!groupingConcept.isSet()) {
					analysis.addWarning(Context.getMessageSourceService().getMessage("htmlformentry.warning.groupingConcept",
					    new Object[] { groupingConceptId }, null));
				}
			}
		}
		return analysis;
	}
	
}

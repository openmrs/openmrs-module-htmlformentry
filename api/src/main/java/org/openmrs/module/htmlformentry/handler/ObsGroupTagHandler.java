package org.openmrs.module.htmlformentry.handler;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Handles the {@code <obsGroup>} tag
 */
public class ObsGroupTagHandler extends AbstractTagHandler {
	
	@Override
	protected List<AttributeDescriptor> createAttributeDescriptors() {
		List<AttributeDescriptor> attributeDescriptors = new ArrayList<AttributeDescriptor>();
		attributeDescriptors.add(new AttributeDescriptor("groupingConceptId", Concept.class));
		attributeDescriptors.add(new AttributeDescriptor("hiddenConceptId", Concept.class));
		attributeDescriptors.add(new AttributeDescriptor("hiddenAnswerConceptId", Concept.class));
		attributeDescriptors.add(new AttributeDescriptor("hiddenAnswer", String.class));
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
		
		// handle the "hidden" obs that may be associated with this obs group
		Concept hiddenQuestion = StringUtils.isNotBlank(attributes.get("hiddenConceptId"))
		        ? HtmlFormEntryUtil.getConcept(attributes.get("hiddenConceptId"))
		        : null;
		String hiddenAnswerStr = StringUtils.isNotBlank(attributes.get("hiddenAnswerConceptId"))
		        ? attributes.get("hiddenAnswerConceptId")
		        : (StringUtils.isNotBlank(attributes.get("hiddenAnswer")) ? attributes.get("hiddenAnswer") : null);
		
		if ((hiddenQuestion != null && hiddenAnswerStr == null) || (hiddenQuestion == null && hiddenAnswerStr != null)) {
			throw new BadFormDesignException("hiddenConcept and hiddenAnswer must be used together");
		}
		
		Object hiddenAnswer = null;
		if (hiddenQuestion != null) {
			if (hiddenQuestion.getDatatype().isNumeric()) {
				try {
					hiddenAnswer = Double.parseDouble(hiddenAnswerStr);
				}
				catch (NumberFormatException e) {
					throw new BadFormDesignException("hiddenAnswer must be a number for numeric datatype");
				}
			}
			if (hiddenQuestion.getDatatype().isText()) {
				hiddenAnswer = hiddenAnswerStr;
			}
			if (hiddenQuestion.getDatatype().isCoded()) {
				hiddenAnswer = HtmlFormEntryUtil.getConcept(hiddenAnswerStr);
				if (hiddenAnswer == null) {
					throw new BadFormDesignException(
					        "could not find concept " + hiddenAnswerStr + " as hiddenAnswer for an obsgroup tag");
				}
			}
		}
		
		boolean ignoreIfEmpty = session.getContext().getMode() == Mode.VIEW && "false".equals(attributes.get("showIfEmpty"));
		
		// avoid lazy init exception
		groupingConcept.getDatatype().getHl7Abbreviation();
		
		String name = attributes.get("label");
		// find relevant obs group to display for this element
		Obs thisGroup = findObsGroup(session, node, attributes.get("groupingConceptId"), hiddenQuestion, hiddenAnswer);
		
		boolean digDeeper = true;
		
		if (thisGroup == null
		        && (session.getContext().getMode() == Mode.EDIT || session.getContext().getMode() == Mode.VIEW)) {
			if (!session.getContext().isUnmatchedMode()) {
				ObsGroupEntity obsGroupEntity = new ObsGroupEntity();
				obsGroupEntity.setPath(ObsGroupComponent.getObsGroupPath(node));
				obsGroupEntity.setQuestionsAndAnswers(hiddenQuestion != null && hiddenQuestion.getDatatype().isCoded()
				        ? ObsGroupComponent.findQuestionsAndAnswersForGroup(attributes.get("groupingConceptId"),
				            hiddenQuestion, (Concept) hiddenAnswer, node)
				        : ObsGroupComponent.findQuestionsAndAnswersForGroup(attributes.get("groupingConceptId"), node));
				obsGroupEntity.setXmlObsGroupConcept(attributes.get("groupingConceptId"));
				obsGroupEntity.setGroupingConcept(groupingConcept);
				obsGroupEntity.setNode(node);
				int unmatchedObsGroupId = session.getContext().addUnmatchedObsGroupEntities(obsGroupEntity);
				out.print(String.format("<unmatched id=\"%s\" />", unmatchedObsGroupId));
				digDeeper = false;
			}
		}
		
		if (ignoreIfEmpty && thisGroup == null) {
			digDeeper = false;
		}
		
		// sets up the obs group stack, sets current obs group to this one
		ObsGroup ogSchemaObj;
		ogSchemaObj = new ObsGroup(groupingConcept, name);
		ogSchemaObj.setHiddenQuestion(hiddenQuestion);
		ogSchemaObj.setHiddenAnswer(hiddenAnswer);
		session.getContext().beginObsGroup(groupingConcept, thisGroup, ogSchemaObj);
		//adds the obsgroup action to the controller stack
		session.getSubmissionController().addAction(ObsGroupAction.start(groupingConcept, thisGroup, ogSchemaObj));
		return digDeeper;
	}
	
	private Obs findObsGroup(FormEntrySession session, Node node, String parentGroupingConceptId, Concept hiddenQuestion,
	        Object hiddenAnswer) {
		String path = ObsGroupComponent.getObsGroupPath(node);
		
		List<ObsGroupComponent> questionsAndAnswers = hiddenQuestion != null && hiddenQuestion.getDatatype().isCoded()
		        ? ObsGroupComponent.findQuestionsAndAnswersForGroup(parentGroupingConceptId, hiddenQuestion,
		            (Concept) hiddenAnswer, node)
		        : ObsGroupComponent.findQuestionsAndAnswersForGroup(parentGroupingConceptId, node);
		
		if (session.getContext().isUnmatchedMode()) {
			return session.getContext().getNextUnmatchedObsGroup(questionsAndAnswers, path);
		} else {
			return session.getContext().findBestMatchingObsGroup(questionsAndAnswers, path);
		}
		
	}
	
	@Override
	public void doEndTag(FormEntrySession session, PrintWriter out, Node parent, Node node) {
		session.getSubmissionController().addAction(ObsGroupAction.end(session.getContext().getActiveObsGroup()));
		session.getContext().endObsGroup();
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
